/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.myexcel.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author liaochong
 * @version 1.0
 */
public class Tr {

    /**
     * 索引
     */
    public int index;
    /**
     * 行单元格
     */
    public List<Td> tdList = Collections.emptyList();
    /**
     * 最大宽度
     */
    public Map<Integer, Integer> colWidthMap = Collections.emptyMap();
    /**
     * 是否可见
     */
    public boolean visibility = true;
    /**
     * 行高度
     */
    public int height;
    /**
     * 是否来源于模板
     */
    public boolean fromTemplate;
    /**
     * 是否在thead中
     */
    public boolean thead;

    public Tr(int index, int height) {
        this.index = index;
        this.height = height;
    }

    public Tr(int index, int height, boolean fromTemplate) {
        this.index = index;
        this.height = height;
        this.fromTemplate = fromTemplate;
    }
}
