package com.rename.dex;

import com.google.common.io.Resources;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Main {
    private static Map<String, CompareClassDef> mClassDefMap = new LinkedHashMap<String, CompareClassDef>();
    private static Map<String, CompareClassDef> mRealClassDefMap = new HashMap<String, CompareClassDef>();
    private static Map<String, ClassDefHandler> mClassDefHandlerMap = new HashMap<String, ClassDefHandler>();

    public static void main(String[] args) {

        try {
            File smaliFile = new File("C:\\Users\\LukeSkyWalker\\IdeaProjects\\RenameDex\\namelines.txt");
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
            mRealClassDefMap.clear();
            mClassDefHandlerMap.clear();
            for (ClassDef classDef : newClasses) {
                CompareClassDef compareClassDef = new CompareClassDef(classDef);
                String source = compareClassDef.getSourceFile();
                if (source == null || !source.endsWith(".java")) {
                    continue;
                }
                String type = compareClassDef.getType();
                String realOuterType = compareClassDef.getRealOuterType();
                mClassDefMap.put(type, compareClassDef);
                ClassDefHandler classDefHandler = mClassDefHandlerMap.get(realOuterType);
                if (classDefHandler == null) {
                    classDefHandler = new ClassDefHandler();
                    mClassDefHandlerMap.put(realOuterType, classDefHandler);
                }
                classDefHandler.add(compareClassDef);
                if (mRealClassDefMap.containsKey(realOuterType)) {
                    if (compareClassDef.isSameName()) {
                        mRealClassDefMap.put(realOuterType, compareClassDef);
                        classDefHandler.setOuterClass(compareClassDef);
                    }
                } else {
                    if (!compareClassDef.isSubClass()) {
                        mRealClassDefMap.put(realOuterType, compareClassDef);
                        classDefHandler.setOuterClass(compareClassDef);
                    }
                }

            }

            for (CompareClassDef compareClassDef : mClassDefMap.values()) {
                writer.write(compareClassDef.getType() + "=" + compareClassDef.getRealType() + "\n");
            }


            DexRewriter rewriter = new DexRewriter(new RewriterModule() {
                public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
                    return new Rewriter<String>() {
                        public String rewrite(String value) {
                            return getRealType(value);
                        }
                    };
                }
            });
            DexFile rewrittenDexFile = rewriter.rewriteDexFile(dexFile);
            DexFileFactory.writeDexFile("C:\\Users\\LukeSkyWalker\\IdeaProjects\\RenameDex\\new.dex", rewrittenDexFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


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
            String key = value.substring(1);
            if (mClassDefMap.containsKey(key)) {
                return "[" + mClassDefMap.get(key).getRealType();
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
