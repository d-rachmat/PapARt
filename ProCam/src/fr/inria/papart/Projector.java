package fr.inria.papart;


import codeanticode.glgraphics.GLGraphicsOffScreen;
import codeanticode.glgraphics.GLTexture;
import codeanticode.glgraphics.GLTextureFilter;
import com.googlecode.javacv.ProjectorDevice;
import java.util.ArrayList;
import javax.media.opengl.GL;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PMatrix3D;


public class Projector{


    private PApplet parent;

    public GLGraphicsOffScreen graphics;
    public ArrayList<Screen> screens;
    
    
    // TODO: this has to be useless.
    protected GLTexture finalImage; 

    // Projector information
    protected ProjectorDevice proj;
    protected PMatrix3D projIntrinsicsP3D, projExtrinsicsP3D, projExtrinsicsP3DInv;

    // Resolution
    protected int frameWidth, frameHeight;

    // OpenGL information
    public float[] projectionMatrixGL = new float[16];
    protected GLTexture myMap;
    public PMatrix3D modelview1;
    protected PMatrix3D projectionInit;
    protected GLTextureFilter lensFilter;
    private GL gl = null;
    
    /**
     * Projector allows the use of a projector for Spatial Augmented reality setup. 
     * This class creates an OpenGL context which allows 3D projection.
     * 
     * @param parent
     * @param calibrationYAML calibration file : OpenCV format
     * @param width  resolution X
     * @param height resolution Y
     * @param near   OpenGL near plane (in mm) or the units used for calibration.
     * @param far    OpenGL far plane  (in mm) or the units used for calibration.
     */
    public Projector(PApplet parent, String calibrationYAML, 
             int width, int height, 
            float near, float far){
        this(parent, calibrationYAML,  width, height,near, far, 0);
    }
    
    public Projector(PApplet parent, String calibrationYAML,
             int width, int height, float near, float far, int AA){

	frameWidth = width;
	frameHeight = height;
	this.parent = parent;

        // create the offscreen rendering for this projector.
        if(AA > 0){
        graphics = new GLGraphicsOffScreen(parent, width, height, true, AA);
        }else{
        graphics = new GLGraphicsOffScreen(parent, width, height);
        }
        loadInternalParams(calibrationYAML);
	initProjection(near, far);
	initModelView();
	initDistortMap(proj);
    }

    
    private void loadInternalParams(String calibrationYAML){
        	// Load the camera parameters. 
	try{
	    
	    ProjectorDevice[] p = ProjectorDevice.read(calibrationYAML);
	    if (p.length > 0) 
		proj = p[0];

	    double[] projMat = proj.cameraMatrix.get();
	    double[] projR = proj.R.get();
	    double[] projT = proj.T.get();
	    projIntrinsicsP3D = new PMatrix3D((float) projMat[0], (float) projMat[1], (float) projMat[2], 0,
					      (float) projMat[3], (float) projMat[4], (float) projMat[5], 0,
					      (float) projMat[6], (float) projMat[7], (float) projMat[8], 0,
					      0, 0, 0, 1);
	    projExtrinsicsP3D = new PMatrix3D((float) projR[0], (float) projR[1], (float) projR[2], (float) projT[0],
					      (float) projR[3], (float) projR[4], (float) projR[5], (float) projT[1], 
					      (float) projR[6], (float) projR[7], (float) projR[8], (float) projT[2],
					      0, 0, 0, 1);

	    projExtrinsicsP3DInv = projExtrinsicsP3D.get();
	    projExtrinsicsP3DInv.invert();

            
            // TODO: get these from somewhere...
//	    double[] camMat = cameraDevice.cameraMatrix.get();
//
//	    camIntrinsicsP3D = new PMatrix3D((float) camMat[0], (float) camMat[1], (float) camMat[2], 0,
//					      (float) camMat[3], (float) camMat[4], (float) camMat[5], 0,
//					      (float) camMat[6], (float) camMat[7], (float) camMat[8], 0,
//					      0, 0, 0, 1);

	}  catch(Exception e){ 
	    // TODO: Exception creation !!
            System.out.println("Error !!!!!");
            System.err.println("Error reading the calibration file : " + calibrationYAML + " \n" + e);
	}
    }
    
    protected void initProjection(float near, float far){
	float p00, p11, p02, p12;

	// ----------- OPENGL --------------
        // Reusing the internal projector parameters for the scene rendering.
        
        p00 = 2*projIntrinsicsP3D.m00 / frameWidth ;
	p11 = 2*projIntrinsicsP3D.m11 / frameHeight ;
	p02 = -(2*projIntrinsicsP3D.m02 / frameWidth  -1);
	p12 = -(2*projIntrinsicsP3D.m12 / frameHeight -1);

	graphics.beginDraw();

        // TODO: magic numbers !!!
	graphics.frustum(0, 0, 0, 0, near, far);
	graphics.projection.m00 = p00;
	graphics.projection.m11 = p11;
	graphics.projection.m02 = p02;
	graphics.projection.m12 = p12;

        // Save these good parameters
	projectionInit = graphics.projection.get();


	graphics.projection.transpose();
	graphics.projection.get(projectionMatrixGL);
	graphics.projection.transpose();
	graphics.endDraw();

    }
        
    protected void initModelView(){
    	graphics.beginDraw();
	graphics.clear(0);
	graphics.resetMatrix();

	graphics.scale(1, 1, -1);
	graphics.modelview.apply(projExtrinsicsP3D);
	modelview1 = graphics.modelview.get();
    }
    
    // Actual GLGraphics BUG :  projection has to be loaded directly into OpenGL.
    protected void loadProjection(){
	gl = graphics.beginGL();
	gl.glMatrixMode(GL.GL_PROJECTION);
	gl.glLoadMatrixf(projectionMatrixGL, 0);
	gl.glMatrixMode(GL.GL_MODELVIEW);
	graphics.endGL();
    }
    
    private void loadModelView(){
        graphics.beginDraw();
        graphics.modelview.set(getModelview1());
        graphics.endDraw();
    }
        
    public void loadGraphics(){

	graphics.beginDraw();
	graphics.clear(0);
	graphics.endDraw();

        loadProjection();
        loadModelView();
   }

    // TODO: un truc genre hasTouch // classe héritant
    public void loadTouch(){
	for(Screen screen: screens){
	    screen.initTouch(this);
	}        
    }
    
    /**
     * This function initializes the distorsion map used by the distorsion shader. 
     * The texture is of the size of the projector resolution.
     * @param proj 
     */
    private void initDistortMap(ProjectorDevice proj){
	lensFilter = new GLTextureFilter(parent, "projDistort.xml");
	finalImage = new GLTexture(parent, frameWidth, frameHeight);

        myMap = new GLTexture(parent, frameWidth, frameHeight, GLTexture.FLOAT);
	float[] mapTmp = new float[ frameWidth *  frameHeight *3];
	int k =0;
	for(int y=0; y < frameHeight ; y++){
	    for(int x=0; x < frameWidth ; x++){
		
		double[] out = proj.undistort(x,y);
		mapTmp[k++] = (float) out[0] / frameWidth;
		mapTmp[k++] = (float) out[1] / frameHeight;
		mapTmp[k++] = 0;
	    }
	}
	myMap.putBuffer(mapTmp, 3);
    }
    
    public PImage distortImageDraw(){
	GLTexture off = graphics.getTexture();

        loadGraphics();
        
	// TODO: depth test ?
	// Setting the scene
	for(Screen screen: screens){
	    //	    GLTexture off2 = shadowMapScreen.getTexture();
	    GLTexture off2 = screen.getTexture();
	    graphics.pushMatrix();
	    graphics.modelview.apply(screen.getPos()); 
	    graphics.image(off2, 0, 0, screen.getSize().x, screen.getSize().y);	    
	    graphics.popMatrix();
	}
	graphics.endDraw();

	// DISTORTION SHADER
	off = graphics.getTexture();
	lensFilter.apply(new GLTexture[]{off, myMap}, finalImage);
	return finalImage;
//        parent.image(finalImage, posX, posY, frameWidth, frameHeight);
    }

    public void addScreen(Screen s){
        screens.add(s);
    }


    GLGraphicsOffScreen getGraphics() {
        return this.graphics;
    }

    PMatrix3D getModelview1() {
        return this.modelview1;
    }

    PMatrix3D getProjectionInit() {
        return this.projectionInit;
    }

    ProjectorDevice getProjectorDevice() {
        return this.proj;
    }
    
    //    /**
//     * TODO: find the use of this function ?? 
//     * 
//     * @param position
//     * @return 
//     */
//    public PVector computePosOnPaper(PVector position){
//	  graphics.pushMatrix();
//	  PVector ret = new PVector();   
//	  graphics.translate(position.x, position.y, position.z);
//	  projExtrinsicsP3DInv.mult(new PVector(graphics.modelview.m03, 
//						graphics.modelview.m13, 
//						-graphics.modelview.m23),
//				    ret);   
//	  graphics.popMatrix();
//	  return ret;
//    }

    public int getWidth() {
        return frameWidth;
    }
    
    public int getHeight() {
        return frameHeight;
    }

}