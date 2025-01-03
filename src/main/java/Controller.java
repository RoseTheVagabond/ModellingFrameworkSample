import models.Bind;

import javax.script.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class Controller {
    private final Object model;
    private final ScriptEngine groovy;
    private final Bindings bindings;

    public Controller(String modelName) throws Exception {
        this.model = Class.forName("models." + modelName).getDeclaredConstructor().newInstance();
        ScriptEngineManager manager = new ScriptEngineManager();
        this.groovy = manager.getEngineByName("groovy");
        this.bindings = new SimpleBindings();
        bindModelFields();
    }

    private void bindModelFields() {
        for (Field field : model.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Bind.class)) {
                field.setAccessible(true);
                try {
                    bindings.put(field.getName(), field.get(model));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Controller readDataFrom(String fname) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(fname))) {
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.isEmpty()) return this;

            // Fill the LATA row based on LL(yearCount)
            String[] yearNames = firstLine.trim().split("\\s+");
            int yearCount = yearNames.length - 1;
            int[] lata = new int[yearCount];

            for (int i = 0; i < yearCount; i++) {
                lata[i] = Integer.parseInt(yearNames[i + 1]);
            }
            bindings.put("LATA", lata);

            // Fill the remaining model variables
            Map<String, double[]> dataMap = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 1) {
                    double[] values = new double[yearCount];
                    Arrays.fill(values, 0.0);
                    for (int i = 1; i < parts.length; i++) {
                        values[i - 1] = Double.parseDouble(parts[i]);
                    }
                    for (int i = parts.length - 1; i < yearCount; i++) {
                        values[i] = values[parts.length - 2];
                    }
                    dataMap.put(parts[0], values);
                }
            }
            updateModelFields(dataMap, yearCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    private void updateModelFields(Map<String, double[]> dataMap, int yearCount) {
        Arrays.stream(model.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Bind.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        if (field.getName().equals("LL")) {
                            field.set(model, yearCount);
                        } else {
                            field.set(model, dataMap.getOrDefault(field.getName(), new double[yearCount]));
                        }
                        bindings.put(field.getName(), field.get(model));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
    }

    public Controller runModel() {
        try {
            Method runMethod = model.getClass().getMethod("run");
            runMethod.invoke(model);
            bindModelFields();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Controller runScriptFromFile(String fname) {
        try {
            String script = Files.readString(Path.of(fname));
            groovy.eval(script, bindings);
        } catch (IOException | ScriptException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Controller runScript(String script) throws ScriptException {
        try {
            groovy.eval(script, bindings);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return this;
    }

    public String getResultsAsTsv() {
        StringBuilder tsvBuilder = new StringBuilder();

        // First, add LATA values as the first line
        int[] lataValues = (int[]) bindings.get("LATA");
        if (lataValues != null) {
            tsvBuilder.append("LATA");
            for (int value : lataValues) {
                tsvBuilder.append("\t").append(value);
            }
            tsvBuilder.append("\n");
        }

        // Process model fields
        Arrays.stream(model.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Bind.class) &&
                        field.getName().length() > 1 &&
                        !field.getName().matches("[a-z]") &&
                        !field.getName().equals("LL"))
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(model);
                        String valueStr = formatValue(value);
                        tsvBuilder.append(field.getName()).append("\t").append(valueStr).append("\n");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });

        // Process additional bindings not in model fields
        bindings.forEach((key, value) -> {
            if (!Arrays.stream(model.getClass().getDeclaredFields())
                    .anyMatch(field -> field.getName().equals(key)) &&
                    !key.matches("[a-z]") &&
                    !key.equals("LATA") &&
                    !key.equals("LL")) {
                String valueStr = formatValue(value);
                tsvBuilder.append(key).append("\t").append(valueStr).append("\n");
            }
        });

        return tsvBuilder.toString();
    }

    // Helper method to format array and other values
    private String formatValue(Object value) {
        if (value instanceof double[]) {
            double[] doubleArray = (double[]) value;
            return Arrays.stream(doubleArray)
                    .mapToObj(this::formatNumber)
                    .collect(Collectors.joining("\t"));
        }
        return formatNumber(value);
    }

    private String formatNumber(Object value) {
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(' ');

            // Check if number is whole (has no decimal part)
            if (number == Math.floor(number)) {
                DecimalFormat wholeFormatter = new DecimalFormat("#,##0", symbols);
                return wholeFormatter.format(number);
            }

            // Format with 2 decimal places
            DecimalFormat decimalFormatter = new DecimalFormat("#,##0.00", symbols);
            String formatted = decimalFormatter.format(number);

            // Remove decimal places if they're all zeros
            if (formatted.endsWith(",00")) {
                return formatted.substring(0, formatted.length() - 3);
            }

            return formatted.replace('.', ',');
        }
        return String.valueOf(value);
    }
}