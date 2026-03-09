package cn.zfz.pureioc.core;
import org.yaml.snakeyaml.Yaml;
import java.util.*;


/**
 * 极简嵌套式YAML配置加载器
 * 特性：
 * 1. 加载application.yml + application-{active}.yml
 * 2. 命令行参数覆盖（支持嵌套key：spring.profiles.active）
 * 3. 自动类型适配（String/Integer/Long/Boolean）
 * 4. List类型合并（而非覆盖）
 * 5. 全空指针安全，无ClassCastException
 */
public final class ConfigLoader {

    // YAML解析器单例（线程安全）
    private static final Yaml YAML = new Yaml();

    

    /**
     * 加载并合并所有配置
     * @param mainArgs main方法启动参数（如--active=dev、--server.port=9090）
     * @return 嵌套结构的最终配置Map（无null值）
     */
    public static Map<String, Object> loadConfig(String baseConfig,String[] mainArgs) {
        // 初始化最终配置（LinkedHashMap保证顺序）
        Map<String, Object> finalConfig = new LinkedHashMap<>();

        // 1. 加载基础配置 application.yml
        Map<String, Object> ymlConfig = loadYamlResource(baseConfig);
        finalConfig.putAll(ymlConfig);

        // 2. 解析启动参数为扁平键值对
        Map<String, String> argsMap = parseMainArguments(mainArgs);

        // 3. 确定激活的环境（命令行优先，其次是配置文件）
        String activeProfile = determineActiveProfile(argsMap, finalConfig);

        // 4. 加载并合并环境配置 application-{active}.yml
        if (activeProfile != null && !activeProfile.isBlank()) {
            String envConfigFile = "app-" + activeProfile + ".yml";
            Map<String, Object> envConfig = loadYamlResource(envConfigFile);
            deepMergeConfig(finalConfig, envConfig);
        }

        // 5. 启动参数覆盖所有配置（最高优先级）
        applyFlatArgsToNestedMap(finalConfig, argsMap);

        return finalConfig;
    }

    /**
     * 从类路径加载YAML文件，返回嵌套Map（空安全）
     */
    private static Map<String, Object> loadYamlResource(String fileName) {
        // 空文件直接返回空Map
        if (fileName == null || fileName.isBlank()) {
            return new LinkedHashMap<>();
        }

        try (var inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            // 文件不存在返回空Map
            if (inputStream == null) {
                return new LinkedHashMap<>();
            }
            // 解析YAML（SnakeYAML返回null表示空文件）
            Map<String, Object> yamlMap = YAML.load(inputStream);
            return yamlMap == null ? new LinkedHashMap<>() : yamlMap;
        } catch (Exception e) {
            // 解析失败返回空Map（避免程序崩溃）
            return new LinkedHashMap<>();
        }
    }

    /**
     * 解析main启动参数：--key=value → 扁平Map
     */
    private static Map<String, String> parseMainArguments(String[] args) {
        Map<String, String> argsMap = new HashMap<>();
        // 空参数直接返回
        if (args == null || args.length == 0) {
            return argsMap;
        }

        for (String arg : args) {
            // 只处理--开头的参数
            if (arg != null && arg.startsWith("--")) {
                String[] kv = arg.substring(2).split("=", 2);
                // 确保是合法的key=value格式
                if (kv.length == 2 && kv[0] != null && kv[1] != null) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    // 跳过空key
                    if (!key.isBlank()) {
                        argsMap.put(key, value);
                    }
                }
            }
        }
        return argsMap;
    }

    /**
     * 确定激活的环境：
     * 1. 优先取命令行--active参数
     * 2. 其次取配置文件spring.profiles.active
     */
    private static String determineActiveProfile(Map<String, String> argsMap, Map<String, Object> config) {
        // 1. 优先读取命令行--active参数
        if (argsMap.containsKey("active")) {
            String active = argsMap.get("active");
            return active == null ? null : active.trim();
        }

        // 2. 读取配置文件中的spring.profiles.active（全类型安全校验）
        Object springNode = config.get("pure");
        if (springNode instanceof Map<?, ?> springMap) {
            Object profilesNode = springMap.get("profiles");
            if (profilesNode instanceof Map<?, ?> profilesMap) {
                Object activeObj = profilesMap.get("active");
                // 确保是字符串类型（避免List/数字等异常类型）
                if (activeObj instanceof String activeStr) {
                    return activeStr.isBlank() ? null : activeStr.trim();
                }
            }
        }

        // 无激活环境
        return null;
    }

    /**
     * 深度合并两个嵌套Map：
     * 1. Map类型递归合并
     * 2. List类型追加合并
     * 3. 基础类型直接覆盖
     */
    @SuppressWarnings("unchecked")
    private static void deepMergeConfig(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);

            // 跳过null值（避免覆盖已有有效值）
            if (sourceValue == null) {
                continue;
            }

            // 场景1：目标和源都是Map → 递归合并
            if (targetValue instanceof Map && sourceValue instanceof Map) {
                deepMergeConfig((Map<String, Object>) targetValue, (Map<String, Object>) sourceValue);
            }
            // 场景2：目标和源都是List → 追加合并
            else if (targetValue instanceof List && sourceValue instanceof List) {
                ((List<Object>) targetValue).addAll((List<Object>) sourceValue);
            }
            // 场景3：基础类型/其他类型 → 直接覆盖
            else {
                target.put(key, sourceValue);
            }
        }
    }

    /**
     * 将扁平的命令行参数转换为嵌套Map并覆盖配置：
     * 例：spring.profiles.active=dev → 嵌套到spring→profiles→active
     * 自动做类型适配（String→Integer/Long/Boolean）
     */
    @SuppressWarnings("unchecked")
    public static void applyFlatArgsToNestedMap(Map<String, Object> nestedConfig, Map<String, String> flatArgs) {
        for (Map.Entry<String, String> entry : flatArgs.entrySet()) {
            String flatKey = entry.getKey();
            String rawValue = entry.getValue();

            // 跳过空key/空value
            if (flatKey.isBlank() || rawValue == null) {
                continue;
            }

            // 1. 拆分扁平key（如spring.profiles.active → ["spring", "profiles", "active"]）
            String[] keyParts = flatKey.split("\\.");
            Map<String, Object> currentLevel = nestedConfig;

            // 2. 逐层创建/定位嵌套Map（直到倒数第二层）
            for (int i = 0; i < keyParts.length - 1; i++) {
                String part = keyParts[i];
                Object nextLevel = currentLevel.get(part);

                // 层级不存在 → 创建空Map
                if (nextLevel == null || !(nextLevel instanceof Map)) {
                    Map<String, Object> newLevel = new LinkedHashMap<>();
                    currentLevel.put(part, newLevel);
                    currentLevel = newLevel;
                }
                // 层级存在 → 继续向下
                else {
                    currentLevel = (Map<String, Object>) nextLevel;
                }
            }

            // 3. 类型适配后设置最终值
            String lastKey = keyParts[keyParts.length - 1];
            Object convertedValue = convertValue(rawValue.trim());
            currentLevel.put(lastKey, convertedValue);
        }
    }

    /**
     * 字符串值自动类型转换：
     * - 数字 → Integer/Long
     * - 布尔 → Boolean
     * - 其他 → String
     */
    private static Object convertValue(String value) {
        // 布尔类型
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // 整数类型（优先Integer，超出范围转Long）
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e2) {
                // 非数字/布尔 → 保留原字符串
                return value;
            }
        }
    }
}
