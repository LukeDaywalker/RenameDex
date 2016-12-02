package com.rename.dex;

import com.google.common.io.Resources;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.rewriter.*;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Main {
    private static Map<String, String> mPackageMap = new HashMap<String, String>();
    private static Map<String, CompareClassDef> mClassDefMap = new LinkedHashMap<String, CompareClassDef>();
    private static Map<String, CompareClassDef> mSourceClassDefMap = new HashMap<String, CompareClassDef>();
    private static Map<String, ClassDef> mNewClassDefMap = new HashMap<String, ClassDef>();
    private static Map<String, ClassDefHandler> mClassDefHandlerMap = new HashMap<String, ClassDefHandler>();

    public static void main(String[] args) {

        try {
            String textFilePath=String.format("%s%spackages.txt", "text", File.separatorChar);
            FileReader fr = new FileReader(findResource(textFilePath));//获取文件流
            BufferedReader br = new BufferedReader(fr); //将流整体读取。
            mPackageMap.clear();
            String str;
            while ((str = br.readLine()) != null) {//判断是否是最后一行
                String[] packages = str.split("->");
                mPackageMap.put(packages[0], packages[1]);
            }

            File smaliFile = new File("namelines.txt");
            if (!smaliFile.exists()) {
                if (!smaliFile.createNewFile()) {
                    System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                    return;
                }
            }

            BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(smaliFile), "UTF8"));

            final Writer writer = new IndentingWriter(bufWriter);

            // write your code here
            String test = "dex";
            String dexFilePath = String.format("%s%sclasses.dex", test, File.separatorChar);

            DexFile dexFile = null;
            dexFile = DexFileFactory.loadDexFile(findResource(dexFilePath), Opcodes.getDefault());
            Set<? extends ClassDef> oldClasses = dexFile.getClasses();

            List<? extends ClassDef> newClasses = new ArrayList<ClassDef>(oldClasses);
//            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
            Collections.sort(newClasses, new Comparator<ClassDef>() {
                @Override
                public int compare(ClassDef o1, ClassDef o2) {
                    CompareClassDef c1 = new CompareClassDef(o1);
                    CompareClassDef c2 = new CompareClassDef(o2);
                    return c1.compareWith(c2);
                }
            });
            mClassDefMap.clear();
            mSourceClassDefMap.clear();
            mClassDefHandlerMap.clear();
            for (ClassDef classDef : newClasses) {
                CompareClassDef compareClassDef = new CompareClassDef(classDef);
                compareClassDef = renamePackage(compareClassDef);
                String type = compareClassDef.getType();
                String sourceType = compareClassDef.getSourceType();
                mClassDefMap.put(type, compareClassDef);
                ClassDefHandler classDefHandler = mClassDefHandlerMap.get(sourceType);
                if (classDefHandler == null) {
                    classDefHandler = new ClassDefHandler();
                    mClassDefHandlerMap.put(sourceType, classDefHandler);
                }
                classDefHandler.add(compareClassDef);
                if (mSourceClassDefMap.containsKey(sourceType)) {
                    if (compareClassDef.isSameName()) {
                        mSourceClassDefMap.put(sourceType, compareClassDef);
                        classDefHandler.setOuterClass(compareClassDef);
                    }
                } else {
                    if (!compareClassDef.isSubClass()) {
                        mSourceClassDefMap.put(sourceType, compareClassDef);
                        classDefHandler.setOuterClass(compareClassDef);
                    }
                }

            }

            for (CompareClassDef compareClassDef : mClassDefMap.values()) {
                writer.write(compareClassDef.getType() + "=" + compareClassDef.getRealType() + "\n");
            }
            writer.flush();
            DexRewriter rewriter = new DexRewriter(new RewriterModule() {

                @Nonnull
                @Override
                public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
                    return new Rewriter<String>() {
                        public String rewrite(String value) {
                            return getRealType(value);
                        }
                    };
                }
            });
            DexFile rewrittenDexFile = rewriter.rewriteDexFile(dexFile);
            mNewClassDefMap.clear();
            for (ClassDef classDef : rewrittenDexFile.getClasses()) {
                mNewClassDefMap.put(classDef.getType(), classDef);
            }
            DexRewriter rewriterField = new DexRewriter(new RewriterModule() {

                @Nonnull
                @Override
                public Rewriter<FieldReference> getFieldReferenceRewriter(@Nonnull Rewriters rewriters) {
                    return new FieldReferenceRewriter(rewriters) {
                        @Nonnull
                        @Override
                        public FieldReference rewrite(@Nonnull FieldReference fieldReference) {
                            return new RewrittenFieldReference(fieldReference) {
                                @Nonnull
                                @Override
                                public String getName() {
                                    return getTypeFieldName(fieldReference);
                                }
                            };
                        }
                    };
                }
            });
            DexFile rewrittenFieldDexFile = rewriterField.rewriteDexFile(rewrittenDexFile);
            DexFileFactory.writeDexFile("classes.dex", rewrittenFieldDexFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }

    private static final int FLAG_NONE = 0;
    private static final int FLAG_FIELD = 1;//有相应的字段则为1
    private static final int FLAG_ERROR = FLAG_FIELD << 1;//有Serializable,或注解有JsonTypeInfo,或属性为public或protected则为10；

    private static int getRenameFieldFlag(ClassDef classDef, FieldReference fieldReference, int flag) {
        if (classDef == null) {
            return flag;
        }

        List<ClassDef> parents = new ArrayList<ClassDef>();

        String parent = classDef.getSuperclass();
        if (hasSerializable(parents, parent)) {//继承
            flag |= FLAG_ERROR;
            return flag;
        }
        List<String> interfaces = classDef.getInterfaces();
        if (interfaces != null) {//接口
            for (String impl : interfaces) {
                if (hasSerializable(parents, impl)) {
                    flag |= FLAG_ERROR;
                    return flag;
                }
            }
        }
        Set<? extends Annotation> annotations = classDef.getAnnotations();
        if (annotations != null) {//判断注解
            for (Annotation annotation : annotations) {
                String annotationType = annotation.getType();
                if (annotationType.equals("Lcom/fasterxml/jackson/annotation/JsonTypeInfo;")) {
                    flag |= FLAG_ERROR;
                    return flag;
                }
            }
        }
        Iterable<? extends Field> fields = classDef.getFields();
        for (Field field : fields) {//查找字段
            if (field.getName().equals(fieldReference.getName())
                    && field.getType().equals(fieldReference.getType())) {
//                if (AccessFlags.PUBLIC.isSet(field.getAccessFlags())
//                        || AccessFlags.PROTECTED.isSet(field.getAccessFlags())) {
//                    flag |= FLAG_ERROR;
//                    return flag;
//                }
                flag |= FLAG_FIELD;
                break;
            }
        }
        for (ClassDef classDefp : parents) {
            flag |= getRenameFieldFlag(classDefp, fieldReference, flag);
            if ((FLAG_ERROR & flag) != 0) {
                return FLAG_ERROR;
            }
        }
        return flag;

    }

    private static boolean hasSerializable(List<ClassDef> parents, String parent) {
        if (parent.equals("Ljava/io/Serializable;")) {
            return true;
        }
        ClassDef classDefI = mNewClassDefMap.get(parent);
        if (classDefI != null) {
            parents.add(classDefI);
        }
        return false;
    }

    private static String getTypeFieldName(FieldReference dexBackedField) {
        String dexBackedFieldName = dexBackedField.getName();
        ClassDef classDef = mNewClassDefMap.get(dexBackedField.getDefiningClass());
        int renameFieldFlag = getRenameFieldFlag(classDef, dexBackedField, FLAG_NONE);
        if ((renameFieldFlag & FLAG_ERROR) != 0 || (renameFieldFlag & FLAG_FIELD) == 0) {
            return dexBackedFieldName;
        }
        if (dexBackedFieldName.length() > 2) {
            return dexBackedFieldName;
        } else if (dexBackedFieldName.length() == 2) {
            char c = dexBackedFieldName.charAt(0);
            if (c < 'a' || c > 'l') {
                return dexBackedFieldName;
            }
        }
        String type = dexBackedField.getType();
        int length = type.length();
        int index = type.lastIndexOf("/");
        int index$ = type.lastIndexOf("$");
        if (index$ > -1) {
            index = index$;
        }
        String typeName;
        if (index > -1) {
            typeName = type.substring(index + 1, length - 1);
        } else {
            typeName = type;
        }

        String name;
        int indexA = type.lastIndexOf("[") + 1;
        if (indexA > 0) {
            if (typeName.contains("[")) {
                if (indexA > 1) {
                    name = "mArray" + indexA + type.substring(indexA) + dexBackedFieldName;
                } else {
                    name = "mArray" + type.substring(indexA) + dexBackedFieldName;
                }
            } else {
                if (indexA > 1) {
                    name = "mArray" + indexA + typeName + dexBackedFieldName;
                } else {
                    name = "mArray" + typeName + dexBackedFieldName;
                }
            }
        } else if (type.equals("Z")) {
            name = "isZ" + dexBackedFieldName;
        } else {
            name = "m" + typeName + dexBackedFieldName;
        }
        return name;
    }

    private static CompareClassDef renamePackage(CompareClassDef compareClassDef) {
        String oldPackage = compareClassDef.getPackage();
        String keyPath = "";
        for (String path : mPackageMap.keySet()) {
            if (oldPackage.startsWith(path)) {
                if (keyPath.length() < path.length()) {
                    keyPath = path;
                }
            }
        }
        if (keyPath.length() > 0) {
            String realPackage = oldPackage.replace(keyPath, mPackageMap.get(keyPath));
            compareClassDef.setRealPackage(realPackage);
        }
        return compareClassDef;
    }

    private static String getRealType(String value) {
        if (value.length() == 1
                || value.startsWith("[") && value.length() == 2) {
            return value;
        }
        if (value.startsWith("L")) {
            if (mClassDefMap.containsKey(value)) {
                return mClassDefMap.get(value).getRealType();
            }
        } else if (value.startsWith("[")) {
            int index = value.lastIndexOf("[") + 1;
            String key = value.substring(index);
            if (mClassDefMap.containsKey(key)) {
                return value.substring(0, index) + mClassDefMap.get(key).getRealType();
            }
        }
        return value;
    }

    @Nonnull
    private static File findResource(String resource) throws URISyntaxException {
        URL resUrl = Resources.getResource(resource);
        return new File(resUrl.toURI());
    }

}
