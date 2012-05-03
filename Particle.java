package galsim;

import java.lang.Math;

public class Particle {

    private final SimulatorStub s;
    private final int index;

    private final int BUFFER_FRACTION = 10;

    // The position that the particle is actually rendered to.
    private float a_x, a_y, a_z;

    private float rotation = 0;

    public Particle(int index, SimulatorStub s) {
        this.index = index;
        this.s = s;
    }

    public float[] get_position() {
        return new float[] {a_x, a_y, a_z};
    }
    public float[] get_rotation() {
        // Get the positions of the particle from the simulator.
        float x, y, z;

        x = s.state[index][0];
        y = s.state[index][1];
        z = s.state[index][2];

        rotation += (Math.abs(x - a_x) + Math.abs(y - a_y) + Math.abs(z - a_z)) / 1000;
        return new float[] {(float)Math.sin(rotation) * 360,
                            (float)Math.cos(rotation) * 360,
                            (float)-Math.sin(rotation) * 360};
    }
    public void update_initial() {
        if(s.state == null)
            return;
        a_x = (float)s.state[index][0];
        a_y = (float)s.state[index][1];
        a_z = (float)s.state[index][2];

        //System.out.println("Particle " + index + ": " + a_x + ", " + a_y + ", " + a_z);
    }
    public void update_position() {
        // Get the positions of the particle from the simulator.
        float x, y, z;

        x = (float)s.state[index][0];
        y = (float)s.state[index][1];
        z = (float)s.state[index][2];

        // Rubberband the particle into the position that it should be
        // rendered at.
        a_x += (x - a_x) / BUFFER_FRACTION;
        a_y += (y - a_y) / BUFFER_FRACTION;
        a_z += (z - a_z) / BUFFER_FRACTION;
        //System.out.println("Particle " + index + ": " + a_x + ", " + a_y + ", " + a_z);
    }
}

