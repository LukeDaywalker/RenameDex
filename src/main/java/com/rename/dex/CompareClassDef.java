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
    private final String mOuterName;
    private final String mInnerName;
    private String mRealType;
    private String mRealPackage;
    private String mRealName;
    private final String mSourceName;
    private final String mSourceType;

    public CompareClassDef(ClassDef classDef) {
        mClassDef = classDef;
        String type = classDef.getType();
        int index = type.lastIndexOf('/');
        mPackage = type.substring(0, index + 1);
        mName = type.substring(index + 1, type.length() - 1);

        int index$ = mName.indexOf("$");
        if (index$ > 0) {
            mOuterName = mName.substring(0, index$);
            mInnerName = mName.substring(index$ + 1);
        } else {
            mOuterName = mName;
            mInnerName = "";
        }

        mRealPackage = mPackage;
        String sourceFile = classDef.getSourceFile();
        if (sourceFile != null) {
            mRealName = sourceFile.substring(0, sourceFile.length() - 5);
            mRealType = mRealPackage + mRealName + ";";
            mSourceName = mRealName;
            mSourceType = mRealType;
        } else {
            mRealName = mName;
            mRealType = type;
            mSourceName = mOuterName;
            mSourceType = type;
        }
    }

    public String getPackage() {
        return mPackage;
    }

    public String getRealPackage() {
        return mRealPackage;
    }

    public void setRealPackage(String realPackage) {
        mRealPackage = realPackage;
        mRealType = mRealPackage + mRealName + ";";
    }

    public void setRealName(String name) {
        mRealName = name;
        mRealType = mRealPackage + mRealName + ";";
    }

    public String getRealName() {
        return mRealName;
    }

    public String getRealType() {
        return mRealType;
    }

    public String getSourceName() {
        return mSourceName;
    }

    public String getSourceType() {
        return mSourceType;
    }

    public String getName() {
        return mName;
    }

    public String getOuterName() {
        return mOuterName;
    }

    public String getInnerName() {
        return mInnerName;
    }

    public boolean isSameName() {
        return mName.equals(mSourceName);
    }

    public boolean hasSameName() {
        return mOuterName.equals(mRealName);
    }

    public boolean isSubClass() {
        return !mInnerName.equals("");
    }

    public void renameClass(CompareClassDef outerClass) {
        if (isSubClass()) {
            if (mOuterName.equals(outerClass.getOuterName())) {//如果
                setRealName(getRegularName(mSourceName + "$" + mInnerName));
            } else {
                setRealName(getRegularName(mSourceName + "$" + mName));
            }
        } else {
            if (getType().equals(outerClass.getType())) {//如果为外部类
                setRealName(getRegularName(mSourceName));
            } else {
                setRealName(getRegularName(mSourceName + "$" + mName));
            }
        }
    }

    private String getRegularName(String name) {
        if (mSourceName.equals("R") && name.length() > 3) {//R文件不做处理
            return name;
        }
        String[] s = name.split("\\$");
        String result = getRegularOneName(s[0]);
        for (int i = 1; i < s.length; i++) {
            result += "$" + getRegularOneName(s[i]);
        }
        return result;
    }

    private String getRegularOneName(String name) {
        if (!isRegularName(name)) {
            name = "IC" + name;
        }
        return name;
    }

    private boolean isRegularName(String name) {
        char c = name.charAt(0);
        return Character.isUpperCase(c) || Character.isDigit(c);
    }


    @Nonnull
    @Override
    public String getType() {
        return mClassDef.getType();
    }

    public int compareWith(CompareClassDef o) {
        int p = comparePackage(getPackage(), o.getPackage());
        if (p == 0) {
            int out = compareString(getOuterName(), o.getOuterName());
            if (out == 0) {
                return compareString(getInnerName(), o.getInnerName());
            }
            return out;
        } else {
            return p;
        }
    }

    private int comparePackage(String name1, String name2) {
        return name1.compareTo(name2);
//        String[] s1 = name1.split("/");
//        String[] s2 = name2.split("/");
//        if (s1.length > s2.length) {
//            for (int i = 0; i < s2.length; i++) {
//                int n = compareString(s1[i], s2[i]);
//                if (n == 0) {
//                    continue;
//                } else {
//                    return n;
//                }
//            }
//            return 1;
//        } else if (s1.length < s2.length) {
//            for (int i = 0; i < s1.length; i++) {
//                int n = compareString(s1[i], s2[i]);
//                if (n == 0) {
//                    continue;
//                } else {
//                    return n;
//                }
//            }
//            return -1;
//        } else {
//            for (int i = 0; i < s1.length; i++) {
//                int n = compareString(s1[i], s2[i]);
//                if (n == 0) {
//                    continue;
//                } else {
//                    return n;
//                }
//            }
//            return 0;
//        }
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
