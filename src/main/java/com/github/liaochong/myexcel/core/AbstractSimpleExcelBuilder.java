/*
 * Copyright 2019 liaochong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.myexcel.core;

import com.github.liaochong.myexcel.core.annotation.ExcelColumn;
import com.github.liaochong.myexcel.core.annotation.ExcludeColumn;
import com.github.liaochong.myexcel.core.annotation.IgnoreColumn;
import com.github.liaochong.myexcel.core.annotation.MultiColumn;
import com.github.liaochong.myexcel.core.constant.BooleanDropDownList;
import com.github.liaochong.myexcel.core.constant.Constants;
import com.github.liaochong.myexcel.core.constant.DropDownList;
import com.github.liaochong.myexcel.core.constant.ImageFile;
import com.github.liaochong.myexcel.core.constant.LinkEmail;
import com.github.liaochong.myexcel.core.constant.LinkUrl;
import com.github.liaochong.myexcel.core.constant.NumberDropDownList;
import com.github.liaochong.myexcel.core.container.Pair;
import com.github.liaochong.myexcel.core.converter.ConvertContext;
import com.github.liaochong.myexcel.core.converter.WriteConverterContext;
import com.github.liaochong.myexcel.core.converter.writer.LocalTimeWriteConverter;
import com.github.liaochong.myexcel.core.parser.ContentTypeEnum;
import com.github.liaochong.myexcel.core.parser.StyleParser;
import com.github.liaochong.myexcel.core.parser.Table;
import com.github.liaochong.myexcel.core.parser.Td;
import com.github.liaochong.myexcel.core.parser.Tr;
import com.github.liaochong.myexcel.core.reflect.ClassFieldContainer;
import com.github.liaochong.myexcel.core.strategy.WidthStrategy;
import com.github.liaochong.myexcel.utils.ConfigurationUtil;
import com.github.liaochong.myexcel.utils.FieldDefinition;
import com.github.liaochong.myexcel.utils.ReflectUtil;
import com.github.liaochong.myexcel.utils.StringUtil;
import com.github.liaochong.myexcel.utils.TdUtil;

import javax.lang.model.type.NullType;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author liaochong
 * @version 1.0
 */
public abstract class AbstractSimpleExcelBuilder {
    /**
     * 字段展示顺序
     */
    protected List<String> fieldDisplayOrder;
    /**
     * 已排序字段
     */
    protected List<FieldDefinition> filteredFields = Collections.emptyList();
    /**
     * 标题
     */
    protected List<String> titles;
    /**
     * 默认值集合
     */
    protected Map<Field, String> defaultValueMap = new HashMap<>();
    /**
     * 自定义宽度
     */
    protected Map<Integer, Integer> customWidthMap = new HashMap<>();
    /**
     * 标题层级
     */
    protected int titleLevel = 0;
    /**
     * 格式化
     */
    private final Map<Integer, String> formats = new HashMap<>();
    /**
     * 是否为Map类型导出
     */
    protected boolean isMapBuild;
    /**
     * 转换上下文
     */
    private final ConvertContext convertContext;

    protected Configuration configuration;

    private final Map<Field, ExcelColumnMapping> excelColumnMappingMap;

    protected StyleParser styleParser = new StyleParser(customWidthMap);

    /**
     * 是否拥有聚合列
     */
    protected boolean hasMultiColumn = false;


    public AbstractSimpleExcelBuilder(boolean isCsvBuild) {
        convertContext = new ConvertContext(isCsvBuild);
        configuration = convertContext.configuration;
        excelColumnMappingMap = convertContext.excelColumnMappingMap;
    }

    /**
     * Core methods for obtaining export related fields, styles, etc
     *
     * @param classFieldContainer classFieldContainer
     * @param groups              分组
     * @return Field
     */
    protected List<FieldDefinition> getFilteredFields(ClassFieldContainer classFieldContainer, Class<?>... groups) {
        ConfigurationUtil.parseConfiguration(classFieldContainer, configuration);
        this.parseGlobalStyle();
        List<FieldDefinition> preElectionFields = this.getPreElectionFields(classFieldContainer);
        List<FieldDefinition> buildFields = this.getGroupFields(preElectionFields, groups);
        // 初始化标题容器
        List<String> titles = new ArrayList<>(buildFields.size());

        for (int i = 0, size = buildFields.size(); i < size; i++) {
            FieldDefinition fieldDefinition = buildFields.get(i);
            Field field = fieldDefinition.getField();
            ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
            String[] columnStyles = null;
            if (excelColumn != null) {
                if (configuration.useFieldNameAsTitle && excelColumn.title().isEmpty()) {
                    titles.add(field.getName());
                } else {
                    titles.add(excelColumn.title());
                }
                if (!excelColumn.defaultValue().isEmpty()) {
                    defaultValueMap.put(field, excelColumn.defaultValue());
                }
                if (excelColumn.width() > -1) {
                    customWidthMap.putIfAbsent(i, excelColumn.width());
                }
                if (excelColumn.style().length > 0) {
                    columnStyles = excelColumn.style();
                }
                if (!excelColumn.format().isEmpty()) {
                    formats.put(i, excelColumn.format());
                } else if (!excelColumn.decimalFormat().isEmpty()) {
                    formats.put(i, excelColumn.decimalFormat());
                } else if (!excelColumn.dateFormatPattern().isEmpty()) {
                    formats.put(i, excelColumn.dateFormatPattern());
                }
                ExcelColumnMapping mapping = ExcelColumnMapping.mapping(excelColumn);
                excelColumnMappingMap.put(fieldDefinition.getField(), mapping);
            } else {
                if (configuration.useFieldNameAsTitle) {
                    titles.add(field.getName());
                } else {
                    titles.add(null);
                }
            }
            styleParser.setColumnStyle(field, i, columnStyles);
            setGlobalFormat(i, field);
        }
        setTitles(titles);
        hasMultiColumn = buildFields.stream().anyMatch(fieldDefinition -> fieldDefinition.getField().isAnnotationPresent(MultiColumn.class));
        return buildFields;
    }

    protected void parseGlobalStyle() {
        styleParser.parse(configuration.style);
    }

    private void setGlobalFormat(int i, Field field) {
        if (formats.get(i) != null) {
            return;
        }
        if (field.getType() == LocalDate.class) {
            formats.put(i, configuration.dateFormat);
        } else if (ReflectUtil.isDate(field.getType())) {
            formats.put(i, configuration.dateTimeFormat);
        } else if (ReflectUtil.isNumber(field.getType())) {
            if (configuration.decimalFormat != null) {
                formats.put(i, configuration.decimalFormat);
            }
        }
    }

    /**
     * 创建table
     *
     * @return table
     */
    protected Table createTable() {
        Table table = new Table();
        table.caption = configuration.sheetName;
        table.trList = new LinkedList<>();
        return table;
    }

    /**
     * 创建标题行
     *
     * @return 标题行
     */
    protected List<Tr> createThead() {
        if (titles == null || titles.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Td>> tdLists = new ArrayList<>();
        // 初始化位置信息
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            if (title == null) {
                continue;
            }
            List<Td> tds = new ArrayList<>();
            String[] multiTitles = title.split(configuration.titleSeparator);
            if (multiTitles.length > titleLevel) {
                titleLevel = multiTitles.length;
            }
            for (int j = 0; j < multiTitles.length; j++) {
                Td td = new Td(j, i);
                td.th = true;
                td.content = multiTitles[j];
                this.setPrompt(td, i);
                this.setImage(td, i);
                this.setDropdownList(td, i);
                tds.add(td);
            }
            tdLists.add(tds);
        }
        // 调整rowSpan
        for (List<Td> tdList : tdLists) {
            Td last = tdList.get(tdList.size() - 1);
            last.setRowSpan(titleLevel - last.row);
        }
        // 调整colSpan
        for (int i = 0; i < titleLevel; i++) {
            int level = i;
            Map<String, List<List<Td>>> groups = tdLists.stream()
                    .filter(list -> list.size() > level)
                    .collect(Collectors.groupingBy(list -> list.get(level).content));
            groups.forEach((k, v) -> {
                if (v.size() == 1) {
                    return;
                }
                List<Td> tds = v.stream().map(list -> list.get(level))
                        .sorted(Comparator.comparing(td -> td.col))
                        .collect(Collectors.toList());

                List<List<Td>> subTds = new LinkedList<>();
                // 不同跨行分别处理
                Map<Integer, List<Td>> partitions = tds.stream().collect(Collectors.groupingBy(td -> td.rowSpan));
                partitions.forEach((col, subTdList) -> {
                    // 区分开不连续列
                    int splitIndex = 0;
                    for (int j = 0, size = subTdList.size() - 1; j < size; j++) {
                        Td current = subTdList.get(j);
                        Td next = subTdList.get(j + 1);
                        if (current.col + 1 != next.col) {
                            List<Td> sub = subTdList.subList(splitIndex, j + 1);
                            splitIndex = j + 1;
                            if (sub.size() <= 1) {
                                continue;
                            }
                            subTds.add(sub);
                        }
                    }
                    subTds.add(subTdList.subList(splitIndex, subTdList.size()));
                });

                subTds.forEach(val -> {
                    if (val.size() == 1) {
                        return;
                    }
                    Td t = val.get(0);
                    t.setColSpan(val.size());
                    for (int j = 1; j < val.size(); j++) {
                        val.get(j).row = -1;
                    }
                });
            });
        }
        Map<Integer, List<Td>> rowTds = tdLists.stream().flatMap(List::stream).filter(td -> td.row > -1).collect(Collectors.groupingBy(td -> td.row));
        List<Tr> trs = new ArrayList<>();
        boolean isComputeAutoWidth = WidthStrategy.isComputeAutoWidth(configuration.widthStrategy);
        rowTds.forEach((k, v) -> {
            Tr tr = new Tr(k, configuration.titleRowHeight);
            tr.colWidthMap = isComputeAutoWidth || !customWidthMap.isEmpty() ? new HashMap<>(titles.size()) : Collections.emptyMap();
            tr.tdList = v.stream().sorted(Comparator.comparing(td -> td.col))
                    .peek(td -> {
                        if (isComputeAutoWidth) {
                            tr.colWidthMap.put(td.col, TdUtil.getStringWidth(td.content, 0.25));
                        }
                    })
                    .collect(Collectors.toList());
            tr.colWidthMap.putAll(customWidthMap);
            trs.add(tr);
        });
        return trs;
    }

    /**
     * 创建内容行
     *
     * @param contents 内容集合
     * @return 内容行
     */
    protected Tr createTr(List<Pair<? extends Class, ?>> contents) {
        Tr tr = new Tr(0, configuration.rowHeight);
        if (contents.isEmpty()) {
            return tr;
        }
        tr.colWidthMap = new HashMap<>();
        List<Td> tdList = IntStream.range(0, contents.size()).mapToObj(index -> {
            Td td = new Td(0, index);
            Pair<? extends Class, ?> pair = contents.get(index);
            if (configuration.autoMerge && pair.getRepeatSize() != null) {
                td.setRowSpan(pair.getRepeatSize());
            }
            this.setTdContent(td, pair);
            this.setTdContentType(td, pair.getKey());
            if (isMapBuild) {
                this.setDateFormatForMap(pair.getKey(), td);
            } else {
                td.format = formats.get(index);
                this.setFormula(index, td);
                this.setPrompt(td, index);
                this.setImage(td, index);
                this.setDropdownList(td, index);
            }
            this.setTdWidth(tr.colWidthMap, td);
            return td;
        }).collect(Collectors.toList());
        tr.colWidthMap.putAll(customWidthMap);
        tr.tdList = tdList;
        return tr;
    }

    private void setTdWidth(Map<Integer, Integer> colWidthMap, Td td) {
        if (!configuration.computeAutoWidth) {
            return;
        }
        if (td.format == null) {
            colWidthMap.put(td.col, TdUtil.getStringWidth(td.content));
        } else {
            if (td.content != null && td.format.length() > td.content.length()) {
                colWidthMap.put(td.col, TdUtil.getStringWidth(td.format));
            } else if (td.date != null || td.localDate != null || td.localDateTime != null) {
                colWidthMap.put(td.col, TdUtil.getStringWidth(td.format, -0.15));
            }
        }
    }

    private void setFormula(int i, Td td) {
        ExcelColumnMapping excelColumnMapping = getExcelColumnMapping(i);
        if (excelColumnMapping == null) {
            return;
        }
        if (excelColumnMapping.formula) {
            td.formula = true;
        }
    }

    protected void setPrompt(Td td, int index) {
        ExcelColumnMapping excelColumnMapping = getExcelColumnMapping(index);
        if (excelColumnMapping == null) {
            return;
        }
        if (excelColumnMapping.promptContainer != null) {
            td.promptContainer = excelColumnMapping.promptContainer;
        }
    }

    protected void setImage(Td td, int index) {
        ExcelColumnMapping excelColumnMapping = getExcelColumnMapping(index);
        if (excelColumnMapping == null) {
            return;
        }
        if (excelColumnMapping.image != null) {
            td.image = excelColumnMapping.image;
        }
    }

    protected void setDropdownList(Td td, int index) {
        ExcelColumnMapping excelColumnMapping = getExcelColumnMapping(index);
        if (excelColumnMapping == null) {
            return;
        }
        if (excelColumnMapping.dropdownList != null) {
            td.dropdownList = excelColumnMapping.dropdownList;
        }
    }

    private ExcelColumnMapping getExcelColumnMapping(int index) {
        if (filteredFields.isEmpty()) {
            return null;
        }
        FieldDefinition fieldDefinition = filteredFields.get(index);
        return excelColumnMappingMap.get(fieldDefinition.getField());
    }

    private void setTdContent(Td td, Pair<? extends Class, ?> pair) {
        Class fieldType = pair.getKey();
        if (fieldType == NullType.class) {
            return;
        }
        if (fieldType == Date.class) {
            td.date = (Date) pair.getValue();
        } else if (fieldType == LocalDateTime.class) {
            td.localDateTime = (LocalDateTime) pair.getValue();
        } else if (fieldType == LocalDate.class) {
            td.localDate = (LocalDate) pair.getValue();
        } else if (com.github.liaochong.myexcel.core.constant.File.class.isAssignableFrom(fieldType)) {
            if (pair.getValue() instanceof File) {
                td.file = (File) pair.getValue();
            } else {
                td.fileIs = (InputStream) pair.getValue();
            }
        } else {
            td.content = String.valueOf(pair.getValue());
        }
    }

    private void setTdContentType(Td td, Class fieldType) {
        if (String.class == fieldType) {
            return;
        }
        if (ReflectUtil.isNumber(fieldType)) {
            td.tdContentType = ContentTypeEnum.DOUBLE;
            return;
        }
        if (ReflectUtil.isDate(fieldType)) {
            td.tdContentType = ContentTypeEnum.DATE;
            return;
        }
        if (ReflectUtil.isBool(fieldType)) {
            td.tdContentType = ContentTypeEnum.BOOLEAN;
            return;
        }
        if (fieldType == DropDownList.class) {
            td.tdContentType = ContentTypeEnum.DROP_DOWN_LIST;
            return;
        }
        if (fieldType == NumberDropDownList.class) {
            td.tdContentType = ContentTypeEnum.NUMBER_DROP_DOWN_LIST;
            return;
        }
        if (fieldType == BooleanDropDownList.class) {
            td.tdContentType = ContentTypeEnum.BOOLEAN_DROP_DOWN_LIST;
            return;
        }
        if (td.content != null && fieldType == LinkUrl.class) {
            td.tdContentType = ContentTypeEnum.LINK_URL;
            setLinkTd(td);
            return;
        }
        if (td.content != null && fieldType == LinkEmail.class) {
            td.tdContentType = ContentTypeEnum.LINK_EMAIL;
            setLinkTd(td);
            return;
        }
        if ((td.file != null || td.fileIs != null) && fieldType == ImageFile.class) {
            td.tdContentType = ContentTypeEnum.IMAGE;
        }
    }

    private void setLinkTd(Td td) {
        String[] splits = td.content.split(Constants.ARROW);
        if (splits.length == 1) {
            td.link = td.content;
        } else {
            td.content = splits[0];
            td.link = splits[1];
        }
    }

    protected void setTitles(List<String> titles) {
        if (this.titles == null) {
            boolean hasTitle = titles.stream().anyMatch(StringUtil::isNotBlank);
            if (hasTitle) {
                this.titles = titles;
            }
        }
    }

    protected List<FieldDefinition> getGroupFields(List<FieldDefinition> preElectionFields, Class<?>[] groups) {
        List<Class<?>> selectedGroupList = Objects.nonNull(groups) ? Arrays.stream(groups).filter(Objects::nonNull).collect(Collectors.toList()) : Collections.emptyList();
        return preElectionFields.stream()
                .filter(fieldDefinition -> (!fieldDefinition.getField().isAnnotationPresent(ExcludeColumn.class) && !fieldDefinition.getField().isAnnotationPresent(IgnoreColumn.class)) && ReflectUtil.isFieldSelected(selectedGroupList, fieldDefinition))
                .sorted(ReflectUtil::sortFields)
                .collect(Collectors.toList());
    }

    protected List<FieldDefinition> getPreElectionFields(ClassFieldContainer classFieldContainer) {
        if (Objects.nonNull(fieldDisplayOrder) && !fieldDisplayOrder.isEmpty()) {
            this.selfAdaption();
            return fieldDisplayOrder.stream()
                    .map(classFieldContainer::getFieldByName)
                    .collect(Collectors.toList());
        }
        List<FieldDefinition> preElectionFields;
        if (configuration.includeAllField) {
            if (configuration.excludeParent) {
                preElectionFields = classFieldContainer.getDeclaredFields();
            } else {
                preElectionFields = classFieldContainer.getFields();
            }
        } else {
            if (configuration.excludeParent) {
                preElectionFields = classFieldContainer.getDeclaredFields().stream()
                        .filter(fieldDefinition ->
                                fieldDefinition.getField().isAnnotationPresent(ExcelColumn.class)
                                        || fieldDefinition.getField().isAnnotationPresent(MultiColumn.class))
                        .collect(Collectors.toList());
            } else {
                preElectionFields = classFieldContainer.getFieldsByAnnotation(ExcelColumn.class, MultiColumn.class);
            }
        }
        if (configuration.ignoreStaticFields) {
            preElectionFields = preElectionFields.stream()
                    .filter(fieldDefinition -> !Modifier.isStatic(fieldDefinition.getField().getModifiers()))
                    .collect(Collectors.toList());
        }
        this.extractedMultiFieldDefinitions(preElectionFields);
        return preElectionFields;
    }

    /**
     * 提取嵌套的MultiColumn中导出列到顶层
     *
     * @param preElectionFields 预选字段
     */
    private void extractedMultiFieldDefinitions(List<FieldDefinition> preElectionFields) {
        List<FieldDefinition> multiFieldDefinitions = preElectionFields.stream()
                .filter(preElectionField -> preElectionField.getField().isAnnotationPresent(MultiColumn.class) && preElectionField.getField().getType() != List.class)
                .flatMap(preElectionField ->
                        // 这种方式会导致缺失第一个父字段，需要补全
                        ReflectUtil.getWriteFieldDefinitionsOfExcelColumn(preElectionField.getField().getType()).stream()
                                .peek(fieldDefinition -> {
                                    if (fieldDefinition.getParentFields() == null || fieldDefinition.getParentFields().isEmpty()) {
                                        fieldDefinition.setParentFields(Collections.singletonList(preElectionField.getField()));
                                    } else {
                                        fieldDefinition.getParentFields().add(0, preElectionField.getField());
                                    }
                                })
                ).collect(Collectors.toList());
        if (multiFieldDefinitions.isEmpty()) {
            return;
        }
        preElectionFields.removeIf(preElectionField -> preElectionField.getField().isAnnotationPresent(MultiColumn.class) && preElectionField.getField().getType() != List.class);
        preElectionFields.addAll(multiFieldDefinitions);
    }

    /**
     * 展示字段order与标题title长度一致性自适应
     */
    private void selfAdaption() {
        if (titles == null || titles.isEmpty()) {
            return;
        }
        if (fieldDisplayOrder.size() > titles.size()) {
            for (int i = 0, size = fieldDisplayOrder.size() - titles.size(); i < size; i++) {
                titles.add(null);
            }
        }
    }

    /**
     * 获取需要被渲染的内容
     *
     * @param data         数据集合
     * @param sortedFields 排序字段
     * @param <T>          泛型
     * @return 结果集
     */
    protected <T> List<List<Pair<? extends Class, ?>>> getMultiRenderContent(T data, List<FieldDefinition> sortedFields) {
        List<Pair<? extends Class, ?>> convertResult = this.getOriginalRenderContent(data, sortedFields);

        // 获取最大长度
        int maxSize = convertResult.stream()
                .filter(pair -> pair.getValue() instanceof List)
                .map(pair -> ((List<?>) pair.getValue()).size())
                .max(Integer::compareTo)
                .orElse(1);

        List<List<Pair<? extends Class, ?>>> result = new LinkedList<>();
        for (int i = 0; i < maxSize; i++) {
            List<Pair<? extends Class, ?>> row = new LinkedList<>();
            for (Pair<? extends Class, ?> pair : convertResult) {
                if (!(pair.getValue() instanceof List)) {
                    if (configuration.autoMerge) {
                        if (i == 0) {
                            pair.setRepeatSize(maxSize);
                            row.add(pair);
                        } else {
                            row.add(Pair.of(NullType.class, null));
                        }
                    } else {
                        row.add(pair);
                    }
                    continue;
                }
                List<?> list = (List<?>) pair.getValue();
                if (list.size() > i) {
                    row.add((Pair<Class, Object>) list.get(i));
                } else {
                    row.add(Constants.NULL_PAIR);
                }
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 获取需要被渲染的内容
     *
     * @param data         数据集合
     * @param sortedFields 排序字段
     * @param <T>          泛型
     * @return 结果集
     */
    protected <T> LinkedList<Pair<? extends Class, ?>> getOriginalRenderContent(T data, List<FieldDefinition> sortedFields) {
        return sortedFields.stream()
                .map(field -> {
                    Pair<? extends Class, Object> value = WriteConverterContext.convert(field, data, convertContext);
                    if (value.getValue() != null) {
                        return value;
                    }
                    String defaultValue = defaultValueMap.get(field);
                    if (defaultValue != null) {
                        return Pair.of(String.class, defaultValue);
                    }
                    if (configuration.defaultValue != null) {
                        return Pair.of(String.class, configuration.defaultValue);
                    }
                    return value;
                })
                .collect(Collectors.toCollection(LinkedList::new));
    }

    protected List<Pair<? extends Class, ?>> assemblingMapContents(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        List<Pair<? extends Class, ?>> contents = new ArrayList<>(data.size());
        if (fieldDisplayOrder == null) {
            data.forEach((k, v) -> {
                this.doAddToContents(contents, v);
            });
        } else {
            for (String fieldName : fieldDisplayOrder) {
                Object val = data.get(fieldName);
                this.doAddToContents(contents, val);
            }
        }
        return contents;
    }

    private void doAddToContents(List<Pair<? extends Class, ?>> contents, Object v) {
        if (v instanceof Pair && ((Pair) v).getKey() instanceof Class) {
            contents.add((Pair) v);
        } else {
            if (v == null) {
                contents.add(Pair.of(NullType.class, null));
            } else if (v.getClass() == LocalTime.class) {
                contents.add(LocalTimeWriteConverter.doConvertDate((LocalTime) v, Constants.DEFAULT_LOCAL_TIME_FORMAT));
            } else {
                contents.add(Pair.of(v.getClass(), v));
            }
        }
    }

    private void setDateFormatForMap(Class<?> objectClass, Td td) {
        if (objectClass == LocalDateTime.class || objectClass == Date.class) {
            td.format = Constants.DEFAULT_DATE_TIME_FORMAT;
        } else if (objectClass == LocalDate.class) {
            td.format = Constants.DEFAULT_DATE_FORMAT;
        }
    }
}
