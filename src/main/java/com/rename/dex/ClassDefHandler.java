package com.rename.dex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by LukeSkywalker on 2016/11/24.
 */
public class ClassDefHandler {
    private List<CompareClassDef> mClassDefList = new ArrayList<CompareClassDef>();
    private CompareClassDef mOuterClass;

    public CompareClassDef getOuterClass() {
        return mOuterClass;
    }

    public void addOuterClass(CompareClassDef outerClass) {
        setOuterClass(outerClass);
        if (!mClassDefList.contains(outerClass)) {
            mClassDefList.add(outerClass);
        }
    }


    public void setOuterClass(CompareClassDef outerClass) {
        mOuterClass = outerClass;
        for (CompareClassDef compareClassDef : mClassDefList) {
            compareClassDef.convertToSubClass(outerClass);
        }
    }

    public void add(CompareClassDef compareClassDef) {
        if (mOuterClass == null) {
            addOuterClass(compareClassDef);
        } else {
            compareClassDef.convertToSubClass(mOuterClass);
            if (!mClassDefList.contains(compareClassDef)) {
                mClassDefList.add(compareClassDef);
            }
        }
    }
}
