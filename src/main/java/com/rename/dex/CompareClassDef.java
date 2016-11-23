package com.rename.dex;

import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Created by LukeSkywalker on 2016/11/23.
 */
public class CompareClassDef implements ClassDef {
    private final ClassDef mClassDef;
    private final String mPackage;
    private final String mName;
    private String mRealType;
    private String mRealName;

    public CompareClassDef(ClassDef classDef) {
        mClassDef = classDef;
        String type = classDef.getType();
        int index = type.lastIndexOf('/');
        mPackage = type.substring(0, index);
        mName = type.substring(index + 1, type.length() - 1);
        String sourceFile = classDef.getSourceFile();
        if (sourceFile != null) {
            mRealName = sourceFile.substring(0, sourceFile.length() - 5);
            mRealType = mPackage + "/" + mRealName + ";";
        } else {
            mRealName = mName;
            mRealType = type;
        }
    }

    public String getPackage() {
        return mPackage;
    }

    public void setRealName(String name) {
        mRealName = name;
        mRealType = mPackage + "/" + mRealName + ";";
    }

    public String getRealName() {
        return mRealName;
    }

    public String getRealType() {
        return mRealType;
    }

    public String getName() {
        return mName;
    }


    @Nonnull
    @Override
    public String getType() {
        return mClassDef.getType();
    }

    public int compareWith(CompareClassDef o) {
        int p = compareString(getPackage(), o.getPackage());
        if (p == 0) {
            return compareString(getName(), o.getName());
        } else {
            return p;
        }
    }

    private int compareString(String type1, String type2) {
        int length1 = type1.length();
        int length2 = type2.length();
        if (length1 == length2) {
            for (int i = 0; i < length1; i++) {
                char c1 = type1.charAt(i);
                char c2 = type2.charAt(i);
                if (c1 == c2) {
                    continue;
                } else if (c1 > c2) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return 0;
        } else if (length1 > length2) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public int compareTo(@Nonnull CharSequence o) {
        return mClassDef.compareTo(o);
    }

    @Override
    public int getAccessFlags() {
        return mClassDef.getAccessFlags();
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return mClassDef.getSuperclass();
    }

    @Nonnull
    @Override
    public List<String> getInterfaces() {
        return mClassDef.getInterfaces();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return mClassDef.getSourceFile();
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return mClassDef.getAnnotations();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return mClassDef.getStaticFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return mClassDef.getInstanceFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Field> getFields() {
        return mClassDef.getFields();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        return mClassDef.getDirectMethods();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return mClassDef.getVirtualMethods();
    }

    @Nonnull
    @Override
    public Iterable<? extends Method> getMethods() {
        return mClassDef.getMethods();
    }

    @Override
    public int length() {
        return mClassDef.length();
    }

    @Override
    public char charAt(int index) {
        return mClassDef.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return mClassDef.subSequence(start, end);
    }
}
