package galsim;

import java.io.*;
import java.net.*;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;


public class Intrepid {

    private int particle_count = 2000;
    private final Sensors s;

    private Particle[] particles;

    private FloatBuffer mat_spec;
    private FloatBuffer light_pos;
    private FloatBuffer white_light;
    private FloatBuffer model_amb;

    private final boolean USE_LIGHTING = false;
    private final float CUBE_SIZE = 0.1f;
    private final float MOUSE_SENSITIVITY = 0.2f;

    public Intrepid() {

        Sensors sense = null;
        try {
            sense = new Sensors();
        } catch(IOException ex) {
            System.out.println("Error initializing client connections.");
            System.out.println(ex.getMessage());
        }
        this.s = sense;

        this.particles = new Particle[0];
    }

    public void start() {
        // If there was an error setting up the sensors, just die.
        if(this.s == null)
            return;

        // Set up the display window.
        try {
            Display.setDisplayMode(new DisplayMode(1024, 768));
            Display.setVSyncEnabled(true);
            Display.setTitle("GalSim: Intrepid Visualizer");
            Display.create();
        } catch(LWJGLException e) {
            // Uh oh, spaghetti-o
            e.printStackTrace();
            System.exit(0);
        }

        // Spin up the simulator
        s.start();

        // Set the display up for perspective mode (as opposed to orthogonal
        // mode);
        GL11.glViewport(0, 0, 1024, 768);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(80.0f, 1024.0f / 768.0f,
                           0.1f, 200.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Set up the lighting variables
        if(USE_LIGHTING)
            init_lights();

        // We want things to be nice and smooth
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // How the screen is cleared on every frame
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClearDepth(1.0f);

        // How OpenGL decides what goes in front and what goes behind
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT,
                    GL11.GL_NICEST);

        if(USE_LIGHTING) {
            // Set up the material of the cubes
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, mat_spec);
            GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 50.0f);

            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, light_pos);
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, white_light);
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, white_light);
            GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, model_amb);

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_LIGHT0);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);

            GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT_AND_DIFFUSE);
        }

        // Create an OpenGL display list to cache the drawing of a cube.
        int cubelist = GL11.glGenLists(1);
        GL11.glNewList(cubelist, GL11.GL_COMPILE);
        drawCube();
        GL11.glEndList();

        GL11.glScalef(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE);
        GLU.gluLookAt(
            0.0f, 10.0f, 1.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        long lastMSUpdate = 0;
        long fps = 0;

        // Set up some things for input
        float dx = 0.0f, dy = 0.0f, dw = 0.0f;
        float cam_vangle = 45.0f, cam_hangle = 0.0f, cam_zoom = 1.0f;
        Mouse.setGrabbed(true);

        // The render loop
        while(!Display.isCloseRequested() &&
              !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {

            // If we don't have any information about the simulation yet, deal
            // with that appropriately.
            if(s.state == null || s.state.length == 0)
                continue;
            if(s.state.length != particles.length) {
                // We've gotten new information from the server about the
                // simulation. Update that information and re-initialize the
                // particle list.
                System.out.println("Initializing particles to match server...");
                particle_count = s.state.length;
                Particle[] ps = new Particle[s.state.length];
                for(int i = 0; i < particle_count; i++) {
                    ps[i] = new Particle(i, this.s);
                    ps[i].update_initial();
                }
                particles = ps;
            }

            // Update the display based on the view settings
            dx = Mouse.getDX();
            dy = Mouse.getDY();
            dw = Mouse.getDWheel();

            if(dy != 0.0f) {
                cam_vangle += dy * MOUSE_SENSITIVITY;
                cam_vangle = Math.min(90.0f, Math.max(-90.0f, cam_vangle));
            }
            if(dx != 0.0f)
                cam_hangle += dx * MOUSE_SENSITIVITY;
            if(dw != 0.0f)
                cam_zoom = Math.max(1.0f, cam_zoom + dw * MOUSE_SENSITIVITY);

            GL11.glPushMatrix();
            GL11.glTranslatef(0.0f, -1 * cam_zoom, 0.0f);
            GL11.glRotatef(cam_hangle, 0.0f, 0.0f, 1.0f);
            GL11.glRotatef(cam_vangle, 0.0f, 1.0f, 0.0f);

            // Clear the screen.
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Draw the particles as cubes.
            for(int i = 0; i < particle_count; i++) {
                if(i == 0)
                    GL11.glColor3f(1.0f, 0.2f, 0.2f);
                else
                    GL11.glColor3f(0.5f, 0.5f, 0.5f);

                GL11.glPushMatrix();

                Particle p = particles[i];
                float[] translation = p.get_position();
                GL11.glTranslatef(translation[0], translation[1], translation[2]);
                float[] rotation = p.get_rotation();
                GL11.glRotatef(rotation[0], 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(rotation[1], 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(rotation[2], 0.0f, 0.0f, 1.0f);

                GL11.glCallList(cubelist);

                GL11.glPopMatrix();
            }

            GL11.glPopMatrix();

            // Update the particles with the latest positions.
            for(int i = 0; i < particle_count; i++)
                particles[i].update_position();

            Display.update();

            // Print the FPS to the terminal.
            long now = System.currentTimeMillis();
            if(now - lastMSUpdate > 1000) {
                System.out.println(fps + " frames per second");
                fps = 0;
                lastMSUpdate = now;
            }
            fps++;

        }
        Display.destroy();

        this.s.stop();
    }

    public static void main(String args[]) throws Exception {
        Intrepid i = new Intrepid();
        i.start();
    }

    // AUX FUNCTIONS

    private void init_lights() {
        mat_spec = BufferUtils.createFloatBuffer(4);
        mat_spec.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();

        light_pos = BufferUtils.createFloatBuffer(4);
        light_pos.put(1.0f).put(1.0f).put(1.0f).put(0.0f).flip();

        white_light = BufferUtils.createFloatBuffer(4);
        white_light.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();

        model_amb = BufferUtils.createFloatBuffer(4);
        model_amb.put(0.5f).put(0.5f).put(0.5f).put(1.0f).flip();
    }

    // Cube drawing bit
    // Borrowed in part from Cube.java, written by Ciardhubh
    public static final float[][] cube_vertices = {
        {-0.5f, -0.5f, -0.5f}, // 0
        {0.5f, -0.5f, -0.5f},
        {0.5f, 0.5f, -0.5f},
        {-0.5f, 0.5f, -0.5f}, // 3
        {-0.5f, -0.5f, 0.5f}, // 4
        {0.5f, -0.5f, 0.5f},
        {0.5f, 0.5f, 0.5f},
        {-0.5f, 0.5f, 0.5f} // 7
    };
    public static final float[][] cube_normals = {
        {0, 0, -1},
        {0, 0, 1},
        {0, -1, 0},
        {0, 1, 0},
        {-1, 0, 0},
        {1, 0, 0}
    };
    public static final byte[][] cube_indicies = {
        {0, 3, 2, 1},
        {4, 5, 6, 7},
        {0, 1, 5, 4},
        {3, 7, 6, 2},
        {0, 4, 7, 3},
        {1, 2, 6, 5}
    };
    public void drawCube() {
        for(int i = 0; i < 6; i++) {
            GL11.glBegin(GL11.GL_QUADS);
            for(int m = 0; m < 4; m++) {
                float[] temp = cube_vertices[cube_indicies[i][m]];
                GL11.glNormal3f(cube_normals[i][0], cube_normals[i][1], cube_normals[i][2]);
                GL11.glVertex3f(temp[0], temp[1], temp[2]);
            }
            GL11.glEnd();
        }
    }

}
