# -*- coding: utf-8 -*-
"""
只读解析器：dump 蛊真人天命（1.4.17 源码 / 目标 1.4.18）的**全量**天赋清单。

用途：审查用清单必须是机器真实解析结果，禁止手工抄写 / 估算。
本脚本不写入任何源码，只读两个 java 文件并产出 stdout（markdown 片段）。

解析对象
--------
1. TianfuPool.java
   - BODIES   : body("id","名",tizhi,"desc")              -> Kind=BODY   quality=凡体  cost=BODY_COST
   - 内置词条 : entry("id","名","品质",cost,"desc")        -> Kind=ENTRY  （out.addAll(List.of(...)) 块）
   - SIJUES   : sijue(n,"名","desc") / new Spec("shashen",...,54,...) -> Kind=SIJUE cost=SIJUE_COST
   - JIYUANS  : new Spec("jiyuan_*",...,Kind.JIYUAN,...)  -> Kind=JIYUAN
2. TianfuTalents.java
   - BASE     : 扩展词条表（t(...) 调用）
   - MALICE   : 恶意联动词条表（也进 BY_ID，单独分组，且运行时受 l2hostility 门禁）

输出
----
- markdown 表格（列：id / 中文名 / 品质 / cost / 是否负成本 / Kind / 描述原文 / 定义位置 / 备注）
- 统计：总条目数、Kind 分布、品质分布、cost 分布、负成本清单、重复 id / 重复中文名
- 附录：效果原语明细（id / op / arg / a / b），供"描述 vs 代码"核对
- 口径差额：与历史口径（内置 22 + 扩展 111 = 133；可购 167）逐项对比

用法
----
    $env:PYTHONIOENCODING='utf-8'
    python docs\\audit-1.4.18\\_dump_talents.py
"""

import os
import re
import sys
from collections import Counter, OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(PROJECT, "tianfu", "src", "main", "java", "net", "guzhenren", "tianfu")
TALENTS_JAVA = os.path.join(SRC, "TianfuTalents.java")
POOL_JAVA = os.path.join(SRC, "TianfuPool.java")

KNOWN_QUALITIES = ["极", "神", "天", "地", "玄", "黄", "负"]


# --------------------------------------------------------------------------- 源码预处理

def read_text(path):
    if not os.path.isfile(path):
        sys.exit("找不到源文件: %s" % path)
    with open(path, "r", encoding="utf-8") as fh:
        return fh.read()


def strip_comments(src):
    """去掉 // 与 /* */ 注释，但保留换行，从而行号不变。字符串字面量内的 // 不受影响。"""
    out = []
    i, n = 0, len(src)
    state = None  # None | '"' | "'"
    while i < n:
        ch = src[i]
        if state is not None:
            out.append(ch)
            if ch == "\\" and i + 1 < n:
                out.append(src[i + 1])
                i += 2
                continue
            if ch == state:
                state = None
            i += 1
            continue
        if ch in ('"', "'"):
            state = ch
            out.append(ch)
            i += 1
            continue
        if ch == "/" and i + 1 < n:
            nxt = src[i + 1]
            if nxt == "/":
                while i < n and src[i] != "\n":
                    out.append(" ")
                    i += 1
                continue
            if nxt == "*":
                while i < n and not (src[i] == "*" and i + 1 < n and src[i + 1] == "/"):
                    out.append("\n" if src[i] == "\n" else " ")
                    i += 1
                out.append("  ")
                i += 2
                continue
        out.append(ch)
        i += 1
    return "".join(out)


def line_of(src, pos):
    return src.count("\n", 0, pos) + 1


def match_paren(src, open_idx):
    """open_idx 指向 '('，返回与之匹配的 ')' 之后的位置。跳过字符串字面量。"""
    depth = 0
    i = open_idx
    n = len(src)
    state = None
    while i < n:
        ch = src[i]
        if state is not None:
            if ch == "\\":
                i += 2
                continue
            if ch == state:
                state = None
            i += 1
            continue
        if ch in ('"', "'"):
            state = ch
            i += 1
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise ValueError("括号未闭合，起点 %d" % open_idx)


def split_top_level(text):
    """按顶层逗号切分（尊重 () [] {} 与字符串字面量）。"""
    parts, buf = [], []
    depth = 0
    state = None
    i = 0
    while i < len(text):
        ch = text[i]
        if state is not None:
            buf.append(ch)
            if ch == "\\" and i + 1 < len(text):
                buf.append(text[i + 1])
                i += 2
                continue
            if ch == state:
                state = None
            i += 1
            continue
        if ch in ('"', "'"):
            state = ch
            buf.append(ch)
            i += 1
            continue
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(buf).strip())
            buf = []
            i += 1
            continue
        buf.append(ch)
        i += 1
    tail = "".join(buf).strip()
    if tail:
        parts.append(tail)
    return parts


def unquote(token):
    token = token.strip()
    if len(token) >= 2 and token[0] == '"' and token[-1] == '"':
        body = token[1:-1]
        return (body.replace('\\"', '"').replace("\\n", "\n")
                .replace("\\\\", "\\").replace("\\t", "\t"))
    return token


def call_args(src, start):
    """start 指向函数名起点，返回 (参数列表, 调用结束后的位置)。"""
    p = src.index("(", start)
    end = match_paren(src, p)
    inner = src[p + 1:end - 1]
    return split_top_level(inner), end


# --------------------------------------------------------------------------- 通用模型

class Item(object):
    __slots__ = ("id", "name", "quality", "cost", "kind", "desc", "file", "line", "fx", "note")

    def __init__(self, id_, name, quality, cost, kind, desc, file_, line, fx=None, note=""):
        self.id = id_
        self.name = name
        self.quality = quality
        self.cost = cost
        self.kind = kind
        self.desc = desc
        self.file = file_
        self.line = line
        self.fx = fx or []
        self.note = note

    @property
    def norm_quality(self):
        return self.quality if self.quality in KNOWN_QUALITIES else "其它"


FX_RE = re.compile(r"^([A-Za-z_][A-Za-z0-9_]*)\s*\((.*)\)$", re.S)


def parse_fx(tokens):
    """把 fx 参数 token 解析成 [(helper, [args...])]。"""
    out = []
    for tok in tokens:
        tok = tok.strip()
        if not tok:
            continue
        m = FX_RE.match(tok)
        if not m:
            out.append((tok, []))
            continue
        out.append((m.group(1), [unquote(a) for a in split_top_level(m.group(2))]))
    return out


def num(token, consts):
    token = token.strip()
    if token in consts:
        return consts[token]
    try:
        return int(token)
    except ValueError:
        pass
    try:
        return float(token)
    except ValueError:
        return token


# --------------------------------------------------------------------------- TianfuTalents.java

T_HELPER_FX = {
    "f": "op", "dao": "op", "fld": "op", "fldAdd": "op", "mob": "op",
    "reflect": "op", "onHit": "op", "adaptive": "op", "counter": "op",
    "undying": "op", "aura": "op",
}


def parse_talents():
    raw = read_text(TALENTS_JAVA)
    src = strip_comments(raw)
    groups = OrderedDict()

    for var in ("BASE", "MALICE"):
        decl = re.search(r"List<Talent>\s+" + var + r"\s*=\s*List\.of\s*\(", src)
        if not decl:
            raise SystemExit("未找到 TianfuTalents.%s 声明" % var)
        open_paren = src.index("(", decl.end() - 1)
        end = match_paren(src, open_paren)
        body = src[open_paren + 1:end - 1]

        items = []
        for m in re.finditer(r"(?<![A-Za-z0-9_.])t\s*\(", body):
            args, _ = call_args(body, m.start())
            if len(args) < 5:
                continue
            id_ = unquote(args[0])
            name = unquote(args[1])
            quality = unquote(args[2])
            cost = num(args[3], {})
            desc = unquote(args[4])
            fx = parse_fx(args[5:])
            items.append(Item(id_, name, quality, cost, "ENTRY", desc,
                              os.path.basename(TALENTS_JAVA),
                              line_of(src, open_paren + 1 + m.start()), fx))
        groups[var] = items
    return groups


# --------------------------------------------------------------------------- TianfuPool.java

def parse_pool():
    raw = read_text(POOL_JAVA)
    src = strip_comments(raw)
    consts = {}
    for m in re.finditer(r"public\s+static\s+final\s+int\s+([A-Z_0-9]+)\s*=\s*(-?\d+)\s*;", src):
        consts[m.group(1)] = int(m.group(2))
    consts.setdefault("BODY_COST", 3)
    consts.setdefault("SIJUE_COST", 16)

    groups = OrderedDict()

    def region(pattern):
        m = re.search(pattern, src)
        if not m:
            return None, None
        p = src.index("(", m.end() - 1)
        e = match_paren(src, p)
        return src[p + 1:e - 1], p + 1

    # --- BODIES ---
    body_src, body_off = region(r"List<Spec>\s+BODIES\s*=\s*List\.of\s*\(")
    bodies = []
    if body_src is not None:
        for m in re.finditer(r"(?<![A-Za-z0-9_.])body\s*\(", body_src):
            args, _ = call_args(body_src, m.start())
            if len(args) < 4:
                continue
            bodies.append(Item(unquote(args[0]), unquote(args[1]), "凡体",
                               consts["BODY_COST"], "BODY", unquote(args[3]),
                               os.path.basename(POOL_JAVA),
                               line_of(src, body_off + m.start()),
                               note="tizhi=" + str(num(args[2], consts))))
    groups["POOL_BODY"] = bodies

    # --- 内置词条（out.addAll(List.of(...)) 第一块）---
    ent_src, ent_off = region(r"out\.addAll\s*\(\s*List\.of\s*\(")
    entries = []
    if ent_src is not None:
        for m in re.finditer(r"(?<![A-Za-z0-9_.])entry\s*\(", ent_src):
            args, _ = call_args(ent_src, m.start())
            if len(args) < 5:
                continue
            entries.append(Item(unquote(args[0]), unquote(args[1]), unquote(args[2]),
                                num(args[3], consts), "ENTRY", unquote(args[4]),
                                os.path.basename(POOL_JAVA),
                                line_of(src, ent_off + m.start())))
    groups["POOL_ENTRY"] = entries

    # --- SIJUES ---
    sj_src, sj_off = region(r"List<Spec>\s+SIJUES\s*=\s*List\.of\s*\(")
    sijues = []
    if sj_src is not None:
        cursor = 0
        while True:
            m = re.search(r"(?<![A-Za-z0-9_.])(sijue|new\s+Spec)\s*\(", sj_src[cursor:])
            if not m:
                break
            abs_start = cursor + m.start()
            args, after = call_args(sj_src, abs_start)
            helper = m.group(1)
            if helper == "sijue":
                if len(args) >= 3:
                    n = num(args[0], consts)
                    sijues.append(Item("sijue_" + str(n), unquote(args[1]), "十绝体",
                                       consts["SIJUE_COST"], "SIJUE", unquote(args[2]),
                                       os.path.basename(POOL_JAVA),
                                       line_of(src, sj_off + abs_start),
                                       note="tizhi=" + str(n)))
            else:
                # new Spec("id","名",Kind.SIJUE,"十绝体",SIJUE_COST,54,"desc")
                if len(args) >= 7:
                    sijues.append(Item(unquote(args[0]), unquote(args[1]), unquote(args[3]),
                                       num(args[4], consts), "SIJUE", unquote(args[6]),
                                       os.path.basename(POOL_JAVA),
                                       line_of(src, sj_off + abs_start),
                                       note="tizhi=" + str(num(args[5], consts))))
            cursor = after
    groups["POOL_SIJUE"] = sijues

    # --- JIYUANS ---
    jy_src, jy_off = region(r"List<Spec>\s+JIYUANS\s*=\s*List\.of\s*\(")
    jiyuans = []
    if jy_src is not None:
        for m in re.finditer(r"new\s+Spec\s*\(", jy_src):
            args, _ = call_args(jy_src, m.start())
            if len(args) < 7:
                continue
            jiyuans.append(Item(unquote(args[0]), unquote(args[1]), unquote(args[3]),
                                num(args[4], consts), "JIYUAN", unquote(args[6]),
                                os.path.basename(POOL_JAVA),
                                line_of(src, jy_off + m.start())))
    groups["POOL_JIYUAN"] = jiyuans

    return groups, consts, src


# --------------------------------------------------------------------------- 运行期停用表

def parse_unavailable():
    """TianfuTalents.NATIVE_UNAVAILABLE：运行期被 available() 过滤掉的 id 及原因。"""
    raw = read_text(TALENTS_JAVA)
    src = strip_comments(raw)
    out = OrderedDict()
    m = re.search(r"NATIVE_UNAVAILABLE\s*=\s*new\s+[A-Za-z.]*ConcurrentHashMap<>\s*\(\s*Map\.of\s*\(", src)
    if not m:
        return out
    p = src.index("(", m.end() - 1)
    e = match_paren(src, p)
    inner = src[p + 1:e - 1]
    for pair in re.finditer(r'"([^"]+)"\s*,\s*"([^"]*)"', inner):
        out[pair.group(1)] = pair.group(2)
    return out


# --------------------------------------------------------------------------- 报告

def fmt_cost(cost):
    if isinstance(cost, int):
        return str(cost)
    return str(cost)


def fx_summary(item):
    if not item.fx:
        return ""
    bits = []
    for helper, args in item.fx:
        if helper == "f" and len(args) >= 2:
            bits.append("%s=%s" % (args[0], args[1]))
        elif helper == "dao":
            bits.append("DAOHEN:%s+%s" % (args[0] if args else "?", args[1] if len(args) > 1 else "?"))
        elif helper in ("fld",):
            bits.append("FIELD_MULT:%s*%s" % (args[0], args[1]))
        elif helper == "fldAdd":
            bits.append("FIELD_ADD:%s+%s" % (args[0], args[1]))
        elif helper == "mob":
            bits.append("MOB:%s amp=%s" % (args[0], args[1] if len(args) > 1 else "0"))
        elif helper in ("reflect", "onHit", "adaptive", "counter", "undying", "aura"):
            bits.append("%s(%s)" % (helper, ",".join(args)))
        else:
            bits.append("%s(%s)" % (helper, ",".join(args)))
    return "; ".join(bits)


def emit_table(items, unavailable):
    rows = ["| id | 中文名 | 品质 | cost | 是否负成本 | Kind | 描述原文 | 定义位置 | 备注 |",
            "|---|---|---|---|---|---|---|---|---|"]
    for it in items:
        note = it.note or ""
        if it.id in unavailable:
            note = (note + " " if note else "") + "**运行期停用**（NATIVE_UNAVAILABLE）"
        rows.append("| `%s` | %s | %s | %s | %s | %s | %s | %s:%d | %s |" % (
            it.id, it.name, it.norm_quality, fmt_cost(it.cost),
            "是" if isinstance(it.cost, int) and it.cost < 0 else "否",
            it.kind, it.desc.replace("|", "\\|"), it.file, it.line, note))
    return rows


def main():
    talents = parse_talents()
    pool, consts, pool_src = parse_pool()
    unavailable = parse_unavailable()

    base = talents["BASE"]
    malice = talents["MALICE"]
    bodies = pool["POOL_BODY"]
    pool_entries = pool["POOL_ENTRY"]
    sijues = pool["POOL_SIJUE"]
    jiyuans = pool["POOL_JIYUAN"]

    lines = []
    w = lines.append

    w("## A. 全量天赋清单（脚本生成，勿手改）")
    w("")
    w("> 本表由 `docs/audit-1.4.18/_dump_talents.py` 直接解析源码生成，**非手工抄写**。")
    w("> 解析对象：`tianfu/src/main/java/net/guzhenren/tianfu/TianfuTalents.java`、`TianfuPool.java`。")
    w("> 品质列已归一化为 `极/神/天/地/玄/黄/负/其它`；凡体 / 十绝体 / 机缘 / 出身归入「其它」，原始品质见「备注」或分组标题。")
    w("")

    sections = [
        ("POOL_BODY", "A.1 体质 · 凡体（TianfuPool.BODIES，Kind=BODY）", bodies, "凡体"),
        ("POOL_ENTRY", "A.2 内置词条（TianfuPool 内联 entry(...)，Kind=ENTRY）", pool_entries, "内置词条"),
        ("POOL_SIJUE", "A.3 十绝体 + 杀神（TianfuPool.SIJUES，Kind=SIJUE）", sijues, "十绝体"),
        ("POOL_JIYUAN", "A.4 机缘（TianfuPool.JIYUANS，Kind=JIYUAN，仅存档兼容）", jiyuans, "机缘"),
        ("BASE", "A.5 扩展词条（TianfuTalents.BASE，Kind=ENTRY）", base, "扩展词条"),
        ("MALICE", "A.6 恶意联动词条（TianfuTalents.MALICE，需 l2hostility）", malice, "恶意联动"),
    ]
    for _, title, items, _label in sections:
        w("### " + title)
        w("")
        w("条目数：**%d**" % len(items))
        w("")
        lines.extend(emit_table(items, unavailable))
        w("")

    # ---------------- 统计 ----------------
    purchasable_entries = pool_entries + base          # 词条池实际内容（MALICE 另页）
    all_pool_items = bodies + pool_entries + sijues + jiyuans + base
    all_items = all_pool_items + malice

    w("## B. 统计（脚本实际数）")
    w("")
    w("### B.1 总量")
    w("")
    w("| 口径 | 实际数 | 说明 |")
    w("|---|---|---|")
    w("| TianfuPool 内置词条 | %d | `entry(...)` 字面量 |" % len(pool_entries))
    w("| TianfuTalents 扩展词条（BASE） | %d | `t(...)` 字面量 |" % len(base))
    w("| 词条合计（内置 + 扩展） | **%d** | 与「133 = 22 + 111」口径对比 |" % len(purchasable_entries))
    w("| 凡体（BODY） | %d | |" % len(bodies))
    w("| 十绝体 + 杀神（SIJUE） | %d | 含 1 条杀神 |" % len(sijues))
    w("| 机缘（JIYUAN） | %d | |" % len(jiyuans))
    w("| **可购合计（词条 + 凡体 + 十绝体 + 机缘）** | **%d** | 与「167」口径对比 |" % len(all_pool_items))
    w("| 可购合计（不把杀神算进十绝体） | %d | 历史口径 167 + %d |"
      % (len(all_pool_items) - 1, len(all_pool_items) - 1 - 167))
    w("| 恶意联动词条（MALICE，独立分页） | %d | 不进主池，单独页面 |" % len(malice))
    w("| **源码中条目总数** | **%d** | 含恶意组 |" % len(all_items))
    w("| 运行期停用（NATIVE_UNAVAILABLE） | %d | 实测无法兑现，被 `available()` 摘出池子 |" % len(unavailable))
    w("")
    w("> 上表「可购合计」是**源码字面量**口径，未扣运行期过滤。")
    w("> 运行期真正能买到的是 `可购合计 − NATIVE_UNAVAILABLE − 未装附属的联动条目`：")
    w("> 缺 `l2hostility` 时恶意组 14 条整页不可见；缺 `guzhenren_addon`/`hundao_addon`/`yundao`")
    w("> 时 `bingxue_daoti`/`hundao_ronghe`/`qiyun_yizhi`/`shen_liupai_jianji` 共 4 条被摘除。")
    w("")

    w("### B.2 Kind 分布")
    w("")
    w("| Kind | 条数 |")
    w("|---|---|")
    kc = Counter()
    for it in all_items:
        kc[it.kind] += 1
    for k in ("BODY", "ENTRY", "SIJUE", "JIYUAN"):
        w("| %s | %d |" % (k, kc.get(k, 0)))
    w("| 合计 | %d |" % len(all_items))
    w("")

    w("### B.3 品质分布")
    w("")
    w("| 品质 | 条数 | 其中负成本 |")
    w("|---|---|---|")
    qc = Counter(it.norm_quality for it in all_items)
    qneg = Counter(it.norm_quality for it in all_items if isinstance(it.cost, int) and it.cost < 0)
    for q in KNOWN_QUALITIES + ["其它"]:
        if qc.get(q, 0) == 0:
            continue
        w("| %s | %d | %d |" % (q, qc[q], qneg.get(q, 0)))
    w("| 合计 | %d | %d |" % (len(all_items), sum(qneg.values())))
    w("")

    w("### B.4 cost 分布")
    w("")
    w("| cost | 条数 | 条目 id |")
    w("|---|---|---|")
    cc = OrderedDict()
    for it in all_items:
        cc.setdefault(fmt_cost(it.cost), []).append(it.id)
    for cost in sorted(cc.keys(), key=lambda c: (float(c) if re.match(r"^-?\d+(\.\d+)?$", c) else 0)):
        ids = cc[cost]
        shown = ", ".join("`%s`" % x for x in ids) if len(ids) <= 12 else \
            ", ".join("`%s`" % x for x in ids[:12]) + " …（共 %d 条）" % len(ids)
        w("| %s | %d | %s |" % (cost, len(ids), shown))
    w("")

    w("### B.5 负成本条目清单（买它反而返还点数）")
    w("")
    neg = [it for it in all_items if isinstance(it.cost, int) and it.cost < 0]
    if neg:
        w("| id | 中文名 | cost | 品质 | 描述原文 | 定义位置 |")
        w("|---|---|---|---|---|---|")
        for it in neg:
            w("| `%s` | %s | %d | %s | %s | %s:%d |" % (
                it.id, it.name, it.cost, it.norm_quality,
                it.desc.replace("|", "\\|"), it.file, it.line))
        w("")
        w("负成本合计返还点数：**%d**" % (-sum(it.cost for it in neg)))
    else:
        w("无。")
    w("")

    w("### B.6 重复检查")
    w("")
    idc = Counter(it.id for it in all_items)
    nmc = Counter(it.name for it in all_items)
    dup_id = [k for k, v in idc.items() if v > 1]
    dup_name = [k for k, v in nmc.items() if v > 1]
    if dup_id:
        w("**重复 id（必须处理：`TianfuPool.BY_ID` / `TianfuTalents.BY_ID` 是 Map.put，后者静默覆盖前者）**")
        w("")
        for k in dup_id:
            hits = [(it.file, it.line) for it in all_items if it.id == k]
            w("- `%s`：%d 处 → %s" % (k, idc[k], ", ".join("%s:%d" % h for h in hits)))
    else:
        w("- 重复 id：**0 处**（%d 个 id 全部唯一）" % len(idc))
    w("")
    if dup_name:
        w("**重复中文名**")
        w("")
        for k in dup_name:
            hits = [(it.id, it.file, it.line) for it in all_items if it.name == k]
            w("- 「%s」：%d 处 → %s" % (k, nmc[k], ", ".join("%s(%s:%d)" % h for h in hits)))
    else:
        w("- 重复中文名：**0 处**（%d 个名字全部唯一）" % len(nmc))
    w("")

    w("### B.7 运行期停用清单（NATIVE_UNAVAILABLE）")
    w("")
    w("这些 id 在 `TianfuTalents.NATIVE_UNAVAILABLE` 里，`available()` 会返回 false，")
    w("`specs()` 因此把它们从词条池中摘掉 —— **玩家在 1.4.17 界面上看不到也买不到它们**。")
    w("")
    if unavailable:
        w("| id | 停用原因 | 是否真在词条表内 |")
        w("|---|---|---|")
        for k, v in unavailable.items():
            hit = [it for it in all_items if it.id == k]
            w("| `%s` | %s | %s |" % (k, v, ("是（%s:%d）" % (hit[0].file, hit[0].line)) if hit else "**否**"))
    else:
        w("无。")
    w("")

    # ---------------- 分组 × 品质 交叉表 ----------------
    w("### B.8 分组 × 品质 交叉表（核对源码注释里「黄 20+5 条」这类声明）")
    w("")
    header = "| 分组 | " + " | ".join(KNOWN_QUALITIES + ["其它"]) + " | 合计 |"
    w(header)
    w("|" + "---|" * (len(KNOWN_QUALITIES) + 2))
    for _, title, items, label in sections:
        cc2 = Counter(it.norm_quality for it in items)
        cells = [str(cc2.get(q, 0)) for q in KNOWN_QUALITIES + ["其它"]]
        w("| %s | %s | %d |" % (label, " | ".join(cells), len(items)))
    w("")

    # ---------------- 口径差额 ----------------
    w("## C. 与历史口径的差额（必须写明的部分）")
    w("")
    w("| 口径项 | 历史值 | 脚本实际 | 差额 |")
    w("|---|---|---|---|")
    hist = [("内置词条", 22, len(pool_entries)),
            ("扩展词条", 111, len(base)),
            ("词条合计", 133, len(purchasable_entries)),
            ("凡体", 20, len(bodies)),
            ("十绝体", 10, len(sijues) - 1),
            ("机缘", 4, len(jiyuans)),
            ("可购合计", 167, len(all_pool_items))]
    for label, old, new in hist:
        w("| %s | %s | **%d** | %+d |" % (label, old, new, new - old))
    w("")
    w("> 注：「十绝体」历史口径按 10 计，脚本实测 `SIJUES` 含 1 条**杀神**（`shashen`，tizhi=54），")
    w("> 故 Kind=SIJUE 共 %d 条。上表「十绝体」行的实际值已按 %d − 1 计。" % (len(sijues), len(sijues)))
    w("> 「可购合计」实际值含杀神；不含杀神时为 %d。" % (len(all_pool_items) - 1))
    w("")

    # ---------------- 源码注释里的过期数字 ----------------
    w("### C.1 源码注释里写死的条目数（已过期，脚本核对）")
    w("")
    w("源码多处 javadoc 仍写着旧计数。脚本把它们抓出来与实际数并列：")
    w("")
    w("| 位置 | 注释里写的数 | 声称的对象 | 脚本实测 | 判定 |")
    w("|---|---|---|---|---|")
    raw_t = read_text(TALENTS_JAVA)
    raw_p = read_text(POOL_JAVA)

    def find_line(raw, needle):
        idx = raw.find(needle)
        if idx < 0:
            return None
        return raw.count("\n", 0, idx) + 1

    claims = [
        (raw_t, "TianfuTalents.java", "天赋词条扩展表（%d 条）", 107, "扩展词条", len(base)),
        (raw_t, "TianfuTalents.java", "避免为 %d 条各写一遍 switch", 107, "扩展词条", len(base)),
        (raw_t, "TianfuTalents.java", "与既有 %d 条词条同乘区叠加", 7, "内置词条", len(pool_entries)),
        (raw_p, "TianfuPool.java", "+ %d 条词条（原有", 129, "词条合计", len(purchasable_entries)),
        (raw_p, "TianfuPool.java", "原有 %d + 扩展", 22, "内置词条", len(pool_entries)),
        (raw_p, "TianfuPool.java", "+ 扩展 %d，扩展表见", 107, "扩展词条", len(base)),
    ]
    for raw, fname, tmpl, claimed, what, real in claims:
        needle = tmpl % claimed
        ln = find_line(raw, needle)
        verdict = "**过期**（差 %+d）" % (real - claimed) if ln and real != claimed else ("一致" if ln else "未找到")
        w("| %s:%s | %d | %s | **%d** | %s |" % (
            fname, str(ln) if ln else "?", claimed, what, real, verdict))
    w("")
    w("> 逐档品质分组的注释声明请对照 **B.8 交叉表** 核对（脚本给出的是实测值）。")
    w("")

    # ---------------- 附录：效果原语 ----------------
    w("## D. 附录 · 效果原语明细（脚本从 fx 参数解析）")
    w("")
    w("用于核对「描述写的 X」与「代码实际落的 Y」是否一致。")
    w("")
    w("| id | 效果原语 |")
    w("|---|---|")
    for it in base + malice:
        if it.fx:
            w("| `%s` | %s |" % (it.id, fx_summary(it).replace("|", "\\|")))
    w("")
    w("内置词条的效果不在 `TianfuTalents` 的效果原语表内 —— 它们是**硬编码**分发在")
    w("`TianfuCombat` / `TianfuEffects` / `TianfuAttributes` 里的 `ids.contains(\"...\")` 分支，")
    w("清单脚本无法从数据表推断，必须在代码里逐条核对（见第二部分审查）。")
    w("")

    print("\n".join(lines))


if __name__ == "__main__":
    main()
