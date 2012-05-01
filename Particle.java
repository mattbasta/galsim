package galsim;

import java.lang.Math;

public class Particle {

    private final ISimulator s;
    private final int index;

    // The position that the particle is actually rendered to.
    private float a_x, a_y, a_z;

    private double rotation = 0;

    public Particle(int index, ISimulator s) {
        this.index = index;
        this.s = s;
    }

    public float[] get_position() {
        return new float[] {a_x, a_y, a_z};
    }
    public float[] get_rotation() {
        // Get the positions of the particle from the simulator.
        double x, y, z;

        double[] state = s.getStateParticle(index);
        x = state[0];
        y = state[1];
        z = state[2];

        rotation += (Math.abs(x - a_x) + Math.abs(y - a_y) + Math.abs(z - a_z)) / 1000;
        return new float[] {(float)Math.sin(rotation) * 360,
                            (float)Math.cos(rotation) * 360,
                            (float)-Math.sin(rotation) * 360};
    }
    public void update_initial() {
        double[] state = s.getStateParticle(index);
        a_x = (float)state[0];
        a_y = (float)state[1];
        a_z = (float)state[2];

        //System.out.println("Particle " + index + ": " + a_x + ", " + a_y + ", " + a_z);
    }
    public void update_position() {
        // Get the positions of the particle from the simulator.
        float x, y, z;

        double[] state = s.getStateParticle(index);
        x = (float)state[0];
        y = (float)state[1];
        z = (float)state[2];

        // Rubberband the particle into the position that it should be
        // rendered at.
        a_x += (x - a_x) / 2;
        a_y += (y - a_y) / 2;
        a_z += (z - a_z) / 2;
        //System.out.println("Particle " + index + ": " + a_x + ", " + a_y + ", " + a_z);
    }
}

