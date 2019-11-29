import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//This is the class that collects samples of the nodes logical clocks at predefine intervals of logical time
public class Sampler implements Runnable {

    private List<Long> logicalClocks;
    private List<String> dnSamplerList;
    private List<Boolean> workCompleted;
    private Set<Integer> runningDNs;

    public Sampler(List<Long> logicalClocks, List<Boolean> workCompleted, Set<Integer> runningDNs) {
        this.dnSamplerList = new ArrayList<>();
        this.logicalClocks = logicalClocks;
        this.workCompleted = workCompleted;
        this.runningDNs = runningDNs;
    }

    public List<Long> getLogicalClocks() {
        return logicalClocks;
    }

    public List<Boolean> getWorkCompleted() {
        return workCompleted;
    }

    public Set<Integer> getRunningDNs() {
        return runningDNs;
    }

    @Override
    public void run() {

        //variable step controls the intervals of cumulative nodes logical time when a sample is collected
        int step = 10;
        boolean allDone;
        long logicalClocksProgress = 0;
        long cumulativeLogicalClocksValue;
        takeASample();
        do {

            //Method areAllTheThreadsDoneWorking checks if there is still a thread working and returns false if there is
            allDone = areAllNodesDoneWorking();

            //if there is still one thread working than it checks if enough cumulative nodes logical time passed
            //to collect a new sample
            if(!allDone) {

                //the progress of the nodes cumulative logical time is calculated using the formula:
                //cumulativeLogicalClocksValue = getCumulativeLogicalClocksValue()/step and since both step and the returned value of
                //getCumulativeLogicalClocksValue method are of int type the quotient value won't change until "step" amount
                //of logical time passes
                cumulativeLogicalClocksValue = getCumulativeLogicalClocksValue()/step;
                if (logicalClocksProgress < cumulativeLogicalClocksValue) {

                    //if cumulativeLogicalClocksValue is greater than the logicalClocksProgress
                    //than it means more then "step" logical units has passed since the last sample was taken
                    //the logicalClocksProgress updates its value with the new cumulativeLogicalClocksValue
                    //and a new sample is taken
                    logicalClocksProgress = cumulativeLogicalClocksValue;
                    takeASample();
                }
            } else {
                //When all threads are done, a last sample with the final value for the node logical clocks is taken
                takeASample();
                saveResultsToFile();
                resetNodesProgressToZero();
                allDone = false;
            }
        } while (!allDone);


    }

    private void resetNodesProgressToZero() {
        System.out.println("Reseting Nodes Progress");
        for(int i=0; i < getWorkCompleted().size(); i++) {
            getWorkCompleted().set(i, false);
            getLogicalClocks().set(i, 0L);
            getRunningDNs().remove(i);
        }
    }


    private void saveResultsToFile() {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("SamplesReport.txt"), "UTF-8"))) {
            out.write("-------------------------------------------------------\n");
            out.write("The samples for this simulation are:\n");
            for (String sample : dnSamplerList) {
                out.write(sample);
                out.newLine();
            }
            out.write("-------------------------------------------------------\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //This method saves the nodes logical clocks values at a certain moment in time
    private void takeASample() {
        String dnSample=" | ";
        int i=0;
        for(Long value : getLogicalClocks()) {
            dnSample =dnSample+"DN "+i+" has LC = "+value+" | ";
            i++;
        }
        dnSamplerList.add(dnSample);
    }

    //This method checks if there is at least one thread still working and returns false if there is
    private boolean areAllNodesDoneWorking() {
        boolean allDone = true;
        for (Boolean isDone:getWorkCompleted()){
            if (!isDone) {
                allDone = false;
                break;
            }
        }

        return allDone;

    }

    //This methods calculates the cumulative value of the nodes logical clocks to be used in determine if its time
    //for a new sample to be taken
    private long getCumulativeLogicalClocksValue() {
        long sum = 0;
        for (Long value : getLogicalClocks()) {
            sum = sum + value;
        }
        return sum;
    }
}
