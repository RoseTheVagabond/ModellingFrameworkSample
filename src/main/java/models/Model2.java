package models;

public class Model2 {
    @Bind
    private int LL; // number of years

    @Bind
    private double[] growthRatesProduction; // the growth rate of production
    @Bind
    private double[] growthRatesConsumption; // the growth rate of consumption
    @Bind
    private double[] growthRatesSavings; // the growth rate of savings

    @Bind
    private double[] production; // production
    @Bind
    private double[] consumption; // consumption
    @Bind
    private double[] savings; // savings
    @Bind
    private double[] netWealth; // net wealth

    private double temp; // auxiliary field, not part of the data model

    public Model2() {
    }

    /**
     * This method performs calculations based on the model's fields.
     * It simulates how production, consumption, savings, and net wealth evolve over the years.
     */
    public void run() {
        // Initialize arrays
        netWealth = new double[LL];
        netWealth[0] = production[0] - consumption[0] + savings[0];

        for (int t = 1; t < LL; t++) {
            production[t] = growthRatesProduction[t] * production[t - 1];
            consumption[t] = growthRatesConsumption[t] * consumption[t - 1];
            savings[t] = growthRatesSavings[t] * savings[t - 1];
            netWealth[t] = production[t] - consumption[t] + savings[t];
        }
    }
}