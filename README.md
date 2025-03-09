# Java Modeling Framework

A flexible Java framework for running financial and economic simulation models with data input/output capabilities and scripting support.

## Project Overview

This modeling framework allows for the creation, execution, and analysis of simulation models that operate over multiple time periods (e.g., yearly financial projections). The system provides:

1. A modular architecture that separates models from their execution environment
2. Data input from structured text files
3. Model execution with predefined variables
4. Scripting support via Groovy to extend calculations
5. A graphical user interface for easy operation
6. Result presentation in tabular format

## Architecture

The project consists of several key components:

### Model Classes

Located in the `models` package, these classes contain the model definitions and calculations:

- `Model1`: A GDP calculation model that computes values based on private consumption, public consumption, investments, exports, and imports.
- `Model2`: A financial model that calculates net wealth based on production, consumption, and savings values.

Models use the `@Bind` annotation to mark fields that should be accessible by the Controller for data binding and result retrieval.

### Controller

The `Controller` class serves as the central component that:

- Loads model classes dynamically using reflection
- Binds data from input files to model variables
- Executes model calculations
- Facilitates script execution (from files or ad-hoc input)
- Formats and returns calculation results

### Utilities

- `Bind.java`: An annotation interface for marking model fields that should be accessible to the Controller

### GUI

`Main.java` implements a Swing-based graphical user interface with:
- Model selection dropdown
- Data file selection dropdown
- Tabular results display
- Script execution capabilities (from file or ad-hoc input)

## Data Format

Input data files use a specific format:
```
LATA 2020 2021 2022 2023 2024
variableName value1 [value2 ... valueN]
```
Where:
- LATA indicates the calculation periods (years)
- Each subsequent line provides values for a model variable
- If fewer values than periods are provided, the last value is used for remaining periods

## Using the Framework

### Running a Model

1. Select a model from the dropdown
2. Select an input data file
3. Click "Run model"
4. View the results in the table

### Running Scripts

Scripts can be executed to perform additional calculations on model results:

1. After running a model, click "Run script from file" to select a Groovy script file
2. Alternatively, click "Create and run ad hoc script" to enter Groovy code directly

Script variables become available in subsequent models and scripts, except for single-letter lowercase variables (e.g., i, j, k).

## Technical Details

### Key Features

- Dynamic class loading using reflection
- Annotation-based dependency injection for model fields
- Script engine integration with Groovy
- TSV (tab-separated values) formatting for output data

### Dependencies

- Java Runtime Environment
- Groovy scripting engine

## Example

For a model like `Model1` with input file `data1.txt`:
```
LATA 2015 2016 2017 2018 2019
twKI 1.03
twKS 1.04
twINW 1.12
twEKS 1.13
twIMP 1.14
KI 1023752.2
KS 315397
INW 348358
EKS 811108.6
IMP 784342.4
```
Running this through the Controller with:

```java
Controller ctl = new Controller("Model1");
ctl.readDataFrom(dataDir + "data1.txt")
   .runModel();
String res = ctl.getResultsAsTsv();
System.out.println(res);
```
Will produce output with calculated GDP values for each year.

## Implementation Notes

The framework uses reflection to dynamically load model classes and access their fields
The Groovy scripting engine is integrated to allow for dynamic calculations
Results are formatted with custom decimal formatting using comma as decimal separator and space as grouping separator
The GUI is built using Java Swing for cross-platform compatibility

