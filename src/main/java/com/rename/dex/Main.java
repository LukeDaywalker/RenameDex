package com.rename.dex;

import com.google.common.io.Resources;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class Main {

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

            Writer writer = new IndentingWriter(bufWriter);

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
            for (ClassDef classDef : newClasses) {
                CompareClassDef compareClassDef=new CompareClassDef(classDef);
                String className = compareClassDef.getType();
                String source = compareClassDef.getSourceFile();
                if (source == null || !source.endsWith(".java")) {
                    continue;
                }
                String oldName = getOldName(className);
                String newName = getNewName(source);
                if (oldName.contains(newName) || oldName.length() > 3) {
                    continue;
                }
//                oldName=className;
//                newName=source;
                writer.write(oldName + "=" + newName + "\n");
            }


//            DexRewriter rewriter = new DexRewriter(new RewriterModule() {
//                public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
//                    return new Rewriter<String>() {
//                        public String rewrite(String value) {
//                            if (value.equals("Lorg/blah/MyBlah;")) {
//                                return "Lorg/blah/YourBlah;";
//                            }
//                            return value;
//                        }
//                    };
//                }
//            });
//            DexFile rewrittenDexFile = rewriter.rewriteDexFile(dexFile);
//            DexFileFactory.writeDexFile("C:\\Users\\LukeSkyWalker\\IdeaProjects\\RenameDex\\new.dex",rewrittenDexFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


    }

    @Nonnull
    private static File findResource(String resource) throws URISyntaxException {
        URL resUrl = Resources.getResource(resource);
        return new File(resUrl.toURI());
    }

    public static String getOldName(String className) {
        //class names should be passed in the normal dalvik style, with a leading L, a trailing ;, and using
        //'/' as a separator.
        if (className.charAt(0) != 'L' || className.charAt(className.length() - 1) != ';') {
            throw new RuntimeException("Not a valid dalvik class name");
        }

        int packageElementCount = 1;
        for (int i = 1; i < className.length() - 1; i++) {
            if (className.charAt(i) == '/') {
                packageElementCount++;
            }
        }

        String[] packageElements = new String[packageElementCount];
        int elementIndex = 0;
        int elementStart = 1;
        for (int i = 1; i < className.length() - 1; i++) {
            if (className.charAt(i) == '/') {
                //if the first char after the initial L is a '/', or if there are
                //two consecutive '/'
                if (i - elementStart == 0) {
                    throw new RuntimeException("Not a valid dalvik class name");
                }

                packageElements[elementIndex++] = className.substring(elementStart, i);
                elementStart = ++i;
            }
        }

        //at this point, we have added all the package elements to packageElements, but still need to add
        //the final class name. elementStart should point to the beginning of the class name

        //this will be true if the class ends in a '/', i.e. Lsome/package/className/;
        if (elementStart >= className.length() - 1) {
            throw new RuntimeException("Not a valid dalvik class name");
        }

        packageElements[elementIndex] = className.substring(elementStart, className.length() - 1);

        return packageElements[elementIndex];
    }

    public static String getNewName(String source) {
        return source.substring(0, source.length() - 5);
    }
}
