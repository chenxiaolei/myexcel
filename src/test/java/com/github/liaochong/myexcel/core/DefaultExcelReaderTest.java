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

import com.github.liaochong.myexcel.core.pojo.CommonPeople;
import com.github.liaochong.myexcel.core.pojo.Picture;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author liaochong
 * @version 1.0
 */
public class DefaultExcelReaderTest extends BasicTest {

    @Test
    public void readXlsxPicture() throws Exception {
        URL htmlToExcelEampleURL = this.getClass().getResource("/picture.xlsx");
        Path path = Paths.get(htmlToExcelEampleURL.toURI());
        List<Picture> pictures = DefaultExcelReader.of(Picture.class).read(path.toFile());
        System.out.println();
    }

    @Test
    public void readXlsPicture() throws Exception {
        URL htmlToExcelEampleURL = this.getClass().getResource("/picture.xls");
        Path path = Paths.get(htmlToExcelEampleURL.toURI());
        List<Picture> pictures = DefaultExcelReader.of(Picture.class).read(path.toFile());
        System.out.println();
    }

    @Test
    public void readWithSheetName() throws Exception {
        URL htmlToExcelEampleURL = this.getClass().getResource("/common_build.xls");
        Path path = Paths.get(htmlToExcelEampleURL.toURI());

        List<CommonPeople> list = DefaultExcelReader.of(CommonPeople.class).sheet("工作表2").rowFilter(row -> row.getRowNum() > 1).read(path.toFile());
        System.out.println();
    }

    @Test
    public void readStartSheet() throws Exception {
        URL htmlToExcelEampleURL = this.getClass().getResource("/common_build.xls");
        Path path = Paths.get(htmlToExcelEampleURL.toURI());

        List<CommonPeople> list = DefaultExcelReader.of(CommonPeople.class).sheet("工作表2")
                .startSheet(sheet -> System.out.println(sheet.getLastRowNum()))
                .rowFilter(row -> row.getRowNum() > 2).read(path.toFile());
        System.out.println();
    }

    @Test
    public void readThenStartSheet() throws Exception {
        URL htmlToExcelEampleURL = this.getClass().getResource("/common_build.xls");
        Path path = Paths.get(htmlToExcelEampleURL.toURI());

        DefaultExcelReader.of(CommonPeople.class).sheet("工作表2")
                .startSheet(sheet -> System.out.println(sheet.getLastRowNum()))
                .rowFilter(row -> row.getRowNum() > 1).readThen(path.toFile(), d -> {
            System.out.println(d.getCats());
        });
        System.out.println();
    }
}
