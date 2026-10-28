import zipfile

for v in ('0.8.4', '0.8.5'):
    p = r'D:\game\mcmod\1\.minecraft\libraries\org\spongepowered\mixin\%s\mixin-%s.jar' % (v, v)
    z = zipfile.ZipFile(p)
    name = 'org/spongepowered/asm/mixin/MixinEnvironment$CompatibilityLevel.class'
    d = z.read(name)
    print(v, b'JAVA_21' in d, b'JAVA_20' in d, b'JAVA_19' in d)
