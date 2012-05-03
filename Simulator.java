package galsim;

public class Simulator extends SimulatorStub implements Runnable {

    private final int cores;
    private final Universe u;

    private long lastMSUpdate = 0;
    private long ticks = 0;

    private final Runnable callback;

    public Simulator(int diameter, int particles, int cores, Runnable callback) {
        this.cores = cores;
        this.u = new Universe(diameter, particles, cores, this);
        this.callback = callback;
    }

    public void start() {
        this.u.run();
    }

    public void stop() {
        this.u.interrupt();
    }

    public float[][] getState() { // For ISimulator.getState
        return this.state;
    }

    public float[] getStateParticle(int i) { // For ISimulator.getState
        return this.state[i];
    }

    public void run() {
        state = u.getSnapshot();

        long now = System.currentTimeMillis();
        if(now - lastMSUpdate > 1000) {
            System.out.println(ticks + " cycles/second");
            ticks = 0;
            lastMSUpdate = now;
        }

        ticks++;

        callback.run();

        //for(int i = 0; i < state.length; i++)
        //    System.out.println(String.valueOf(state[i][0]) + "," +
        //                       String.valueOf(state[i][1]) + "," +
        //                       String.valueOf(state[i][2]));
    }

}
