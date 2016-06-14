/**
 * Copyright [2015] [soledede]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foofv.crawler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.foofv.crawler.rsmanager.CpuInfo;
import com.foofv.crawler.rsmanager.MemoryInfo;

/**
 * @author soledede
 * @ClassName: ResourseTool
 * @Description: cpu used , memeory used info
 */
public class ResourseTool {

    private static long lastIdleCpuTime = 0;
    private static long lastTotalCpuTime = 0;

    private static String osName = System.getProperty("os.name");

    private static int cpuCores = Runtime.getRuntime().availableProcessors();

    public static MemoryInfo getMemInfo() throws IOException,
            InterruptedException {
        MemoryInfo memInfo = new MemoryInfo();
        if (!osName.toLowerCase().startsWith("windows")) {
            long totalMem = 0;
            long freeMem = 0;


            Process pro = null;
            Runtime r = Runtime.getRuntime();

            String command = "cat /proc/meminfo";
            pro = r.exec(command);

            int count = 0;
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    pro.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] memInfos = line.split("\\s+");
                if (memInfos[0].toLowerCase().startsWith("MemTotal".toLowerCase())) {
                    totalMem = Long.valueOf(memInfos[1]).longValue();
                    memInfo.memTotal_$eq(totalMem);
                    count++;
                } else if (memInfos[0].toLowerCase().startsWith(
                        "MemFree".toLowerCase())) {
                    freeMem = Long.valueOf(memInfos[1]).longValue();
                    memInfo.memFree_$eq(freeMem);
                    count++;
                } else if (memInfos[0].toLowerCase().startsWith(
                        "SwapTotal".toLowerCase())) {
                    memInfo.swapFree_$eq(Long.valueOf(memInfos[1]).longValue());
                    count++;
                } else if (memInfos[0].toLowerCase().startsWith(
                        "SwapFree".toLowerCase())) {
                    memInfo.swapFree_$eq(Long.valueOf(memInfos[1]).longValue());
                    count++;
                }
                if (count == 4) {
                    memInfo.memUsage_$eq(1 - (double) freeMem
                            / (double) totalMem);
                    break;
                }

            }
        }
        return memInfo;

    }

    public static CpuInfo getCpuInfo() throws IOException, InterruptedException,
            RuntimeException {
        if (osName.toLowerCase().startsWith("windows")) return new CpuInfo(ResourseTool.cpuCores, 0.0);
        long[] l = getCpuInfoFromLinux();
        if (l == null)
            throw new RuntimeException();
        if (lastIdleCpuTime == 0 || lastTotalCpuTime == 0) {// first fetch
            lastIdleCpuTime = l[0];
            lastTotalCpuTime = l[1];
            Thread.sleep(1000);
            l = getCpuInfoFromLinux();
        }
        /*CpuInfo cpuInfo = new CpuInfo(ResourseTool.cpuCores, (double) 1 - (l[0] - lastIdleCpuTime)
                / (double) (l[1] - lastTotalCpuTime));*/
        double cpuTotal = l[1] - lastTotalCpuTime;
        double cpuIdle = l[0] - lastIdleCpuTime;
        double cpuUsageRate = (cpuTotal - cpuIdle) / cpuTotal;
        CpuInfo cpuInfo = new CpuInfo(ResourseTool.cpuCores, cpuUsageRate);
        lastIdleCpuTime = l[0];
        lastTotalCpuTime = l[1];
        return cpuInfo;
    }


    private static long[] getCpuInfoFromLinux() throws IOException {
        long[] l = new long[2];
        Process pro;
        Runtime r = Runtime.getRuntime();
        String command = "cat /proc/stat";
        pro = r.exec(command);
        BufferedReader in = new BufferedReader(new InputStreamReader(
                pro.getInputStream()));
        String line = null;
        long idleCpuTime = 0, totalCpuTime = 0;
        while ((line = in.readLine()) != null) {
            if (line.toLowerCase().startsWith("cpu")) {
                line = line.trim();
                String[] temp = line.split("\\s+");
                idleCpuTime += Long.valueOf(temp[4]).longValue();
                for (int i = 0; i < temp.length; i++) {
                    if (!temp[i].toLowerCase().startsWith("cpu")) {
                        totalCpuTime += Long.valueOf(temp[i]).longValue();
                    }
                }
            } else if (idleCpuTime != 0L && totalCpuTime != 0L) {
                l[0] = idleCpuTime;
                l[1] = totalCpuTime;
                break;
            }
        }
        in.close();
        pro.destroy();
        return l;
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("os.name"));
        System.out.println(cpuCores);

    }

    public static void testssss() {
        System.out.println("come in....");
    }

    public static String testS() {
        return "what's";
    }

}
