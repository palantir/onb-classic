/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.onb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.palantir.onb.types.BootRule;
import com.palantir.onb.types.BootRules;
import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

class BootRulesParserTest {

    @Test
    void testStockBlankRules() {
        BootRules bootRules = BootRulesParser.stockBlankRules();
        String stockRules = null;
        try {
            stockRules = bootRules.saveRules();
        } catch (IOException e) {
            fail(e.toString());
        }
        assertNotNull(stockRules);
        BootRulesParser parser = new BootRulesParser(new Logging());
        BootRules bootRules1 = parser.loadRules(stockRules);

        assertNotNull(bootRules1);
    }

    @Test
    void testLastSupported() {
        assertEquals(BootRulesParser.getLastSupported(), "2.0.0");
    }

    private File saveRulesToFile(TemporaryFolder temporaryFolder) {
        File createdFile = null;
        try {
            createdFile = temporaryFolder.createFile("rules.onr");
        } catch (IOException e) {
            fail("Could not save file to temp location, " + e.toString());
        }

        BootRules bootRules = BootRulesParser.stockBlankRules();
        String bootRulesString = null;
        try {
            bootRulesString = bootRules.saveRules();
        } catch (IOException e) {
            fail(e.toString());
        }
        assertTrue(bootRules.saveFile(bootRulesString, createdFile.getAbsolutePath()));
        return createdFile;
    }

    @Test
    @ExtendWith(TemporaryFolderExtension.class)
    void saveRuleFile(TemporaryFolder temporaryFolder) {
        saveRulesToFile(temporaryFolder);
    }

    @Test
    @ExtendWith(TemporaryFolderExtension.class)
    @SuppressWarnings("SpellCheckingInspection")
    void loadRules(TemporaryFolder temporaryFolder) {
        File createdFile = saveRulesToFile(temporaryFolder);
        BootRules bootRules = BootRulesParser.stockBlankRules();
        bootRules.addInterfaces("eth0,eth1");
        BootRule singleRule = new BootRule();
        singleRule.setBootFile("text.efi");
        singleRule.setClient("myClient");
        singleRule.setHardwareAddress("00:00:00:00:00:00");
        int[] testHardware = {1, 7};
        singleRule.setHardwareTypes(testHardware);
        singleRule.setServerIp("129.168.0.1");
        singleRule.setTftpIp("192.168.0.1");
        bootRules.getRuleSet().add(singleRule);
        bootRules.setLastFileLogLoc("C:\\\\log.txt");
        String bootRulesString = null;
        try {
            bootRulesString = bootRules.saveRules();
        } catch (IOException e) {
            fail(e.toString());
        }
        assertTrue(bootRules.saveFile(bootRulesString, createdFile.getAbsolutePath()));

        BootRulesParser bootRulesParser = new BootRulesParser(new Logging());
        String loadedRulesData = bootRulesParser.loadRulesFile(createdFile.getAbsolutePath());
        assertNotNull(bootRulesParser.loadRules(loadedRulesData));

        //noinspection SpellCheckingInspection
                StringBuilder newerTest = new StringBuilder()
                .append("{\n")
                .append("\t\"compVersion\": \"3.0.2\",\n")
                .append("\t\"enablepxe\": true,\n")
                .append("\t\"enabletftp\": true,\n")
                .append("\t\"enablehttp\": true,\n")
                .append("\t\"lastconloglevel\": 4,\n")
                .append("\t\"lastfileloglevel\": 4,\n")
                .append("\t\"lastfilelogloc\": \"log.txt\",\n")
                .append("\t\"lastints\": \"vmnet8,vmnet3,vmnet1,\",\n")
                .append("\t\"httpport\": 80,\n")
                .append("\t\"broadcastmode\": 0,\n")
                .append("\t\"tftp\": {\n")
                .append("\t\t\"rootfolder\": \"./tftpboot\"\n")
                .append("\t},\n")
                .append("\t\"pxerules\": [\\n")
                .append("\t\t{\n")
                .append("\t\t\t\"clientid\": \"iPXE\",\n")
                .append("\t\t\t\"bootfile\": \"menu.ipxe\"\n")
                .append("\t\t},\n")
                .append("\t\t{\n")
                .append("\t\t\t\"arch\": \"7,8,9\",\n")
                .append("\t\t\t\"bootfile\": \"ipxe.efi\"\n")
                .append("\t\t},\n")
                .append("\t\t{\n")
                .append("\t\t\t\"bootfile\": \"undionly.kpxe\"\n")
                .append("\t\t}\n")
                .append("\t]\n")
                .append("}\n");
        assertNull(bootRulesParser.loadRules(newerTest.toString()));

                StringBuilder olderTest = new StringBuilder()
                .append("{\n")
                .append("\t\"compVersion\": \"1.0.0\",\n")
                .append("\t\"enablepxe\": true,\n")
                .append("\t\"enabletftp\": true,\n")
                .append("\t\"enablehttp\": true,\n")
                .append("\t\"lastconloglevel\": 4,\n")
                .append("\t\"lastfileloglevel\": 4,\n")
                .append("\t\"lastfilelogloc\": \"log.txt\",\n")
                .append("\t\"lastints\": \"vmnet8,vmnet3,vmnet1,\",\n")
                .append("\t\"httpport\": 80,\n")
                .append("\t\"broadcastmode\": 0,\n")
                .append("\t\"tftp\": {\n")
                .append("\t\t\"rootfolder\": \"./tftpboot\"\n")
                .append("\t},\n")
                .append("\t\"pxerules\": [\n")
                .append("\t\t{\n")
                .append("\t\t\t\"clientid\": \"iPXE\",\n")
                .append("\t\t\t\"bootfile\": \"menu.ipxe\"\n")
                .append("\t\t},\n")
                .append("\t\t{\n")
                .append("\t\t\t\"arch\": \"7,8,9\",\n")
                .append("\t\t\t\"bootfile\": \"ipxe.efi\"\n")
                .append("\t\t},\n")
                .append("\t\t{\n")
                .append("\t\t\t\"bootfile\": \"undionly.kpxe\"\n")
                .append("\t\t}\n")
                .append("\t]\n")
                .append("}\n");
        assertNull(bootRulesParser.loadRules(olderTest.toString()));
    }
}
