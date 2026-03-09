package cn.zfz.pureioc.utils;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Opcodes;


public class AnnotationUtils {

    /**
     * 判断字节码数组里是否含有指定注解
     * @param classBytes  类字节码
     * @param annotationClass 要判断的注解（如 Component.class, ConditionalOnClass.class）
     * @return 是否存在
     */
    public static boolean hasAnnotation(byte[] classBytes, Class<?> annotationClass) {
        String annotationDesc = "L" + annotationClass.getName().replace('.', '/') + ";";
        final boolean[] found = {false};

        ClassReader cr = new ClassReader(classBytes);
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (annotationDesc.equals(desc)) {
                    found[0] = true;
                }
                return super.visitAnnotation(desc, visible);
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return found[0];
    }
}