package galsim;

public interface ISimulator {

    void start();
    void stop();

    double[][] getState();
    double[] getStateParticle(int i);

}
