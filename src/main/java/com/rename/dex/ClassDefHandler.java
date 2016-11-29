package com.rename.dex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LukeSkywalker on 2016/11/24.
 */
public class ClassDefHandler {
    private List<CompareClassDef> mClassDefList = new ArrayList<CompareClassDef>();
    private CompareClassDef mOuterClass;

    public CompareClassDef getOuterClass() {
        return mOuterClass;
    }


    public void setOuterClass(CompareClassDef outerClass) {
        mOuterClass = outerClass;
        if (!mClassDefList.contains(outerClass)) {
            mClassDefList.add(outerClass);
        }
        for (CompareClassDef compareClassDef : mClassDefList) {
            compareClassDef.renameClass(outerClass);
        }
    }

    public void add(CompareClassDef compareClassDef) {
        if (mOuterClass == null) {
            setOuterClass(compareClassDef);
        } else {
            compareClassDef.renameClass(mOuterClass);
            if (!mClassDefList.contains(compareClassDef)) {
                mClassDefList.add(compareClassDef);
            }
        }
    }
}
