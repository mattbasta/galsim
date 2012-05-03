package galsim;

public class SimulatorStub {

    public float[][] state;

    void start() {}
    void stop() {}

    float[][] getState() {return state;}
    float[] getStateParticle(int i) {return state[i];}

}
