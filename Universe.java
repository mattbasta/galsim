package galsim;

import java.util.concurrent.*;
import java.util.Random;


public class Universe {

    public final int diameter;

    private final int particles;
    private final int cores;
    private final int[] particle_masses;
    private double[][] particle_locations;
    private final double[][] particle_velocities;

    private final CyclicBarrier barrier;
    private final Runnable callback;

    private Thread[] sims;

    final int MAX_MASS = 300;
    final double TIME_PER_TICK = 10;
    final double GRAVITY = Double.valueOf("6.6738480E-11");
    final double SOFTENING = 0.01;

    public Universe(int diameter, int particles, int cores, Runnable callback) {
        this.diameter = diameter;

        this.particles = particles;
        this.particle_masses = new int[particles];
        this.particle_locations = new double[particles][3];
        this.particle_velocities = new double[particles][3];

        this.cores = cores;
        if(cores > 0)
            this.barrier = new CyclicBarrier(cores);
        else
            this.barrier = null;
        this.callback = callback;

        Random rgen = new Random();

        for(int i = 0; i < particles; i++) {
            if(i == 0 && false) {
                this.particle_masses[0] = 60000000;
                this.particle_locations[i][0] = 0;
                this.particle_locations[i][1] = 0;
                this.particle_locations[i][2] = 0;

                this.particle_velocities[i][0] = 0;//rgen.nextDouble() * 0.003 - 0.0015;
                this.particle_velocities[i][1] = 0;//rgen.nextDouble() * 0.003 - 0.0015;
                this.particle_velocities[i][2] = 0;//rgen.nextDouble() * 0.003 - 0.0015;
                continue;
            }
            this.particle_masses[i] = (int)Math.pow(rgen.nextInt(MAX_MASS), 3);

            this.particle_locations[i][0] = rgen.nextInt(diameter) - diameter / 2;
            this.particle_locations[i][1] = rgen.nextInt(diameter) - diameter / 2;
            this.particle_locations[i][2] = rgen.nextInt(diameter) - diameter / 2;

            this.particle_velocities[i][0] = rgen.nextDouble() * 0.003 - 0.0015;
            this.particle_velocities[i][1] = rgen.nextDouble() * 0.003 - 0.0015;
            this.particle_velocities[i][2] = rgen.nextDouble() * 0.003 - 0.0015;
        }

    }

    public double[][] getSnapshot() {
        return particle_locations.clone();
    }

    private void update_particle_location(int particle) {
        double x, y, z;
        x = this.particle_locations[particle][0];
        y = this.particle_locations[particle][1];
        z = this.particle_locations[particle][2];

        // v=d/t
        // delta d=vt
        x += this.particle_velocities[particle][0] * TIME_PER_TICK;
        y += this.particle_velocities[particle][1] * TIME_PER_TICK;
        z += this.particle_velocities[particle][2] * TIME_PER_TICK;

        this.particle_locations[particle] = new double[] {x, y, z};
    }

    private void update_particle_velocity(int particle) {
        int mass = this.particle_masses[particle];

        // Cache the location of the particle in the universe.
        double x, y, z;
        x = this.particle_locations[particle][0];
        y = this.particle_locations[particle][1];
        z = this.particle_locations[particle][2];

        //System.out.println("Working on particle " + particle);

        // Create a new "vector" for this tick of the simulation.
        double vec_x = 0, vec_y = 0, vec_z = 0;

        // Start processing at the next particle. We just take the inverse
        // of the acceleration vector and apply it to the other particles'
        // velocities, so someone else in the past updated our velocity vector.
        for(int i = 0; i < particles; i++) {
            if(i == particle) continue;

            // Get the location information for particle `i`.
            double[] ploc = this.particle_locations[i];

            // Get the non-normalized direction vector from the particle to
            // particle `i`.
            double xd = x - ploc[0], yd = y - ploc[1], zd = z - ploc[2];
            double distance = Math.sqrt(Math.pow(xd, 2) +
                                        Math.pow(yd, 2) +
                                        Math.pow(zd, 2));
            //System.out.println("  " + i + ": " + distance + "units away.");

            // Newton's Universal Law of Gravitation
            // F = G(m_1 * m_2) / r^2

            // F = ma
            // a = delta(v)/delta(t)
            // F = m * delta(v) / delta(t)
            // m * delta(v) / delta(t) = G(m_1 * m_2) / r^2
            // delta(v) / delta(t) = G * m_2 / r^2
            // delta(v) = (G * m_2 * delta(t)) / r^2

            double delta_v = GRAVITY * this.particle_masses[i] * distance;
            delta_v /= Math.pow(
                    Math.pow(distance, 2) + SOFTENING * SOFTENING, 3 / 2);
            // delta_v is now the change in velocity along the unit vector
            // directed from this particle to particle `i`. In order to add
            // delta_v to this particle's velocity, we must multiply it by the
            // unit vector so that each component of the vector accurately
            // represents the intended direction of the velocity.

            vec_x -= (xd / distance) * delta_v;
            vec_y -= (yd / distance) * delta_v;
            vec_z -= (zd / distance) * delta_v;
        }

        // Add the velocity added in the last tick of the simulation to the
        // current velocity vector for the particle.
        this.particle_velocities[particle][0] += vec_x;
        this.particle_velocities[particle][1] += vec_y;
        this.particle_velocities[particle][2] += vec_z;

    }

    class ParticleGroupSimulator implements Runnable {
        private int divisions, offset;
        public ParticleGroupSimulator(int divisions, int offset)
            {this.divisions = divisions; this.offset = offset;}
        public void run() {
            while(true) {
                // Calculate the acceleration for each particle, then apply
                // that to the particle's velocity.
                for(int i = this.offset; i < particles; i += this.divisions)
                    update_particle_velocity(i);

                // Wait for simulations to finish.
                try {barrier.await();}
                catch(InterruptedException ex) {return;}
                catch(BrokenBarrierException ex) {return;}

                // Update the locations from the newly calculated velocities.
                for(int i = this.offset; i < particles; i += this.divisions)
                    update_particle_location(i);

                try {
                    if(barrier.await() == 0) {
                        // Run the callback to pass the data to the network module.
                        callback.run();
                    }
                }
                catch(InterruptedException ex) {
                    System.out.println("Interrupted.");
                    return;}
                catch(BrokenBarrierException ex) {
                    System.out.println("Broken barrier.");
                    return;}
            }
        }
    }

    public void run() {
        if(this.cores < 1)
            simulate_single();
        else
            simulate_multiproc(this.cores);
    }

    private void simulate_single() {
        // Pass back an initial snapshot.
        callback.run();

        ParticleGroupSimulator[] foo = new ParticleGroupSimulator[particles];
        while(true) {
            for(int i = 0; i < particles; i++)
                update_particle_velocity(i);
            for(int i = 0; i < particles; i++)
                update_particle_location(i);
            callback.run();
            /*
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {return;}
            */
        }
    }

    private void simulate_multiproc(int cores) {

        // Pass back an initial snapshot.
        callback.run();

        sims = new Thread[cores];
        for(int i = 0; i < cores; i++) {
            sims[i] = new Thread(new ParticleGroupSimulator(cores, i));
            sims[i].start();
        }
    }

    public void interrupt() {
        if(this.cores > 0)
            for(int i = 0; i < cores; i++)
                sims[i].interrupt();
    }

}
