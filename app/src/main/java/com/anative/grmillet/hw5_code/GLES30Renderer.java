package com.anative.grmillet.hw5_code;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.opengles.GL10;

class AniObject{
    public float pos_x;
    public float pos_y;
    public float pos_z;
    public float movement;
    public String name;
    public float rot_angle;

    public AniObject(float x, float y, float z, float mov, String name, float rot_angle){
        pos_x = x;
        pos_y = y;
        pos_z = z;
        movement = mov;
        this.name = name;
        this.rot_angle = rot_angle;
    }
}
public class GLES30Renderer implements GLSurfaceView.Renderer {

    private Context mContext;

    /* Preparing for Animated Object */
    private AniObject aniMario;
    private AniObject aniBus;
    private AniObject aniBigBike;
    private AniObject aniSmallBike;

    Camera mCamera;
    private Mario mMario;
    private Building mBuilding;
    private Bus mBus;
    private Bike mBikeBig;
    private Bike mBikeSmall;


    public float ratio = 1.0f;
    public int headLightFlag = 1;
    public int lampLightFlag = 1;
    public int pointLightFlag = 1;
    public int cowLightFlag = 1;
    public int textureFlag = 1;

    public float[] mMVPMatrix = new float[16];
    public float[] mProjectionMatrix = new float[16];
    public float[] mModelViewMatrix = new float[16];
    public float[] mModelMatrix = new float[16];
    public float[] mModelMatrix_BigBike = new float[16];
    public float[] mModelMatrix_SmallBike = new float[16];

    public float[] mViewMatrix = new float[16];
    public float[] mModelViewInvTrans = new float[16];

    final static float SCALE_MARIO = 5.0f;
    final static int TEXTURE_ID_MARIO = 0;
    final static int TEXTURE_ID_BIKE = 1;

    final static float SCALE_BUS = 2.0f;
    final static float SCALE_BIGBIKE = 3.0f;
    final static float SCALE_SMALLBIKE = 0.3f;

    final static float INIT_MARIO_X = 5.0f;            final static float INIT_MARIO_Y = 0.0f;      final static float INIT_MARIO_Z = 0.0f;
    final static float INIT_BUS_X = 0.0f;               final static float INIT_BUS_Y = 0.0f;        final static float INIT_BUS_Z = -20.0f;
    final static float INIT_BIGBIKE_X = -10.0f;        final static float INIT_BIGBIKE_Y = 0.0f;   final static float INIT_BIGBIKE_Z = 20.0f;
    final static float INIT_SMALLBIKE_X = 0.0f;        final static float INIT_SMALLBIKE_Y = 2.0f; final static float INIT_SMALLBIKE_Z = -1.0f;
    final static float MARIO_MOV = 0.05f, BUS_MOV = 0.1f, BIKE_MOV=0.1f;
    final static float MARIO_ROT = 0.0f, BUS_ROT = 0.0f, BIKE_ROT=0.0f;

    private ShadingProgram mShadingProgram;

    public GLES30Renderer(Context context) {
        mContext = context;
    }

    @Override
    // Initialize_renderer() : main에서 불러주던 함수
    public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.8f, 1.0f);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        // 각 물체의 초기 위치를 설정
        aniMario = new AniObject(INIT_MARIO_X, INIT_MARIO_Y, INIT_MARIO_Z, MARIO_MOV, "Mario", MARIO_ROT);
        aniBus = new AniObject(INIT_BUS_X, INIT_BUS_Y, INIT_BUS_Z, BUS_MOV, "Bus", BUS_ROT);
        aniBigBike = new AniObject(INIT_BIGBIKE_X, INIT_BIGBIKE_Y, INIT_BIGBIKE_Z, BIKE_MOV, "BigBike", BIKE_ROT);
        aniSmallBike = new AniObject(INIT_SMALLBIKE_X, INIT_SMALLBIKE_Y, INIT_SMALLBIKE_Z, BIKE_MOV, "SmallBike", BIKE_ROT);

        // 초기 뷰 매트릭스를 설정.
        mCamera = new Camera();

        //vertex 정보를 할당할 때 사용할 변수.
        int nBytesPerVertex = 8 * 4;        // 3 for vertex, 3 for normal, 2 for texcoord, 4 is sizeof(float)
        int nBytesPerTriangles = nBytesPerVertex * 3;

        /*
            우리가 만든 ShadingProgram을 실제로 생성하는 부분
         */
        mShadingProgram = new ShadingProgram(
            AssetReader.readFromFile("vertexshader.vert" , mContext),
            AssetReader.readFromFile("fragmentshader.frag" , mContext));
        mShadingProgram.prepare();
        mShadingProgram.initLightsAndMaterial();
        mShadingProgram.initFlags();
        mShadingProgram.set_up_scene_lights(mViewMatrix);

        /*
                우리가 만든 Object들을 로드.
         */
        mMario = new Mario();
        mMario.addGeometry(AssetReader.readGeometry("Mario_Triangle.geom", nBytesPerTriangles, mContext));
        mMario.prepare();
        mMario.setTexture(AssetReader.getBitmapFromFile("mario.jpg", mContext), TEXTURE_ID_MARIO);

        mBuilding = new Building();
        mBuilding.addGeometry(AssetReader.readGeometry("Building1_vnt.geom", nBytesPerTriangles, mContext));
        mBuilding.prepare();
        // No texture for Building

        mBus = new Bus();
        mBus.addGeometry(AssetReader.readGeometry("Bus.geom", nBytesPerTriangles, mContext));
        mBus.prepare();

        mBikeBig = new Bike();
        mBikeBig.addGeometry(AssetReader.readGeometry("Bike.geom", nBytesPerTriangles, mContext));
        mBikeBig.prepare();

        mBikeSmall = new Bike();
        mBikeSmall.addGeometry(AssetReader.readGeometry("Bike.geom", nBytesPerTriangles, mContext));
        mBikeSmall.prepare();
        mBikeSmall.setTexture(AssetReader.getBitmapFromFile("Bike.jpg", mContext), TEXTURE_ID_BIKE);

    }

    // Animate function
    public void moveObject(AniObject obj){
        if(obj.name.equals("Mario")){
            obj.pos_z = 3.0f*(float)Math.cos(Math.toRadians(obj.rot_angle-90.0f));
            obj.pos_x = 3.0f*(float)Math.sin(Math.toRadians(obj.rot_angle-90.0f));
            obj.rot_angle += 1.0f;
            if(obj.rot_angle>=360.0f){
                obj.rot_angle=0.0f;
            }

            /*
            if(obj.pos_z>=5.0){
                obj.movement = -obj.movement;
                obj.rot_angle = 180.0f;
            }
            else if(obj.pos_z<0.0){
                obj.movement = -obj.movement;
                obj.rot_angle = 0.0f;
            }
            obj.pos_z += obj.movement;
            */
        }
        else if(obj.name.equals("Bus")){
            if(obj.pos_x>=5.0){
                obj.movement = -obj.movement;
            }
            else if(obj.pos_x<-5.0){
                obj.movement = -obj.movement;
            }
            obj.pos_x += obj.movement;
        }
        else if(obj.name.equals("BigBike")){
            if(obj.pos_z>=45.0){
                obj.movement = -obj.movement;
            }
            else if(obj.pos_z<INIT_BIGBIKE_Z){
                obj.movement = -obj.movement;
            }
            obj.pos_z += obj.movement;
        }
        else if(obj.name.equals("SmallBike")){
            obj.rot_angle += 1.0f;
            if(obj.rot_angle>=360.0f){
                obj.rot_angle=0.0f;
            }
        }
        else{}
    }

    @Override
    //Display()
    public void onDrawFrame(GL10 gl){ // 그리기 함수 ( = display )
        int pid;
        int timestamp = getTimeStamp();

        /*
             실시간으로 바뀌는 ViewMatrix의 정보를 가져온다.
             MVP 중 V 매트릭스.
         */
        mViewMatrix = mCamera.GetViewMatrix();
        /*
             fovy 변화를 감지하기 위해 PerspectiveMatrix의 정보를 가져온다.
             MVP 중 P
             mat, offset, fovy, ratio, near, far
         */
        Matrix.perspectiveM(mProjectionMatrix, 0, mCamera.getFovy(), ratio, 0.1f, 2000.0f);

        /*
              행렬 계산을 위해 이제 M만 계산하면 된다.
         */

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        mShadingProgram.set_lights1();


        /*
         그리기 영역.
         */
        mShadingProgram.use(); // 이 프로그램을 사용해 그림을 그릴 것입니다.

        Matrix.setIdentityM(mModelMatrix, 0);

        moveObject(aniMario);
        Matrix.translateM(mModelMatrix, 0, aniMario.pos_x+INIT_MARIO_X, aniMario.pos_y, aniMario.pos_z);
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 1f, 0f, 0f);
        Matrix.rotateM(mModelMatrix, 0, 180.0f, 0f, 1f, 0f);
        Matrix.rotateM(mModelMatrix, 0, aniMario.rot_angle, 0f, 0f, 1f);
        Matrix.scaleM(mModelMatrix, 0, SCALE_MARIO, SCALE_MARIO, SCALE_MARIO);


        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);
        Matrix.transposeM(mModelViewInvTrans, 0, mModelViewMatrix, 0);
        Matrix.invertM(mModelViewInvTrans, 0, mModelViewInvTrans, 0);

        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewProjectionMatrix, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrix, 1, false, mModelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrixInvTrans, 1, false, mModelViewInvTrans, 0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mMario.mTexId[0]);
        GLES30.glUniform1i(mShadingProgram.locTexture, TEXTURE_ID_MARIO);

        mShadingProgram.setUpMaterialMario();
        mMario.draw();

        // Draw Building
        Matrix.setIdentityM(mModelMatrix, 0);

        //Matrix.scaleM(mModelMatrix, 0, 0.1f, 0.1f, 0.1f);
        Matrix.rotateM(mModelMatrix, 0, -90.0f, 1f, 0f, 0f);
        Matrix.scaleM(mModelMatrix, 0, 1.0f, 1.0f, 1.0f);
        Matrix.translateM(mModelMatrix, 0, -120.0f, -80.0f, 0.0f);  // size of the building is (230, 160)


        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);
        Matrix.transposeM(mModelViewInvTrans, 0, mModelViewMatrix, 0);
        Matrix.invertM(mModelViewInvTrans, 0, mModelViewInvTrans, 0);

        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewProjectionMatrix, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrix, 1, false, mModelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrixInvTrans, 1, false, mModelViewInvTrans, 0);

        mShadingProgram.setUpMaterialBuilding();
        mBuilding.draw();

        // Draw Bus
        moveObject(aniBus);
        Matrix.setIdentityM(mModelMatrix, 0);

        Matrix.translateM(mModelMatrix, 0, aniBus.pos_x, aniBus.pos_y, aniBus.pos_z);  // center of the building is (120, 80)
        Matrix.scaleM(mModelMatrix, 0, SCALE_BUS, SCALE_BUS, SCALE_BUS);
        Matrix.rotateM(mModelMatrix, 0, 90.0f, 0.0f, 1.0f, 0.0f);

        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);
        Matrix.transposeM(mModelViewInvTrans, 0, mModelViewMatrix, 0);
        Matrix.invertM(mModelViewInvTrans, 0, mModelViewInvTrans, 0);

        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewProjectionMatrix, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrix, 1, false, mModelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrixInvTrans, 1, false, mModelViewInvTrans, 0);

        mShadingProgram.setUpMaterialBus();
        mBus.draw();

        // Draw BigBike first
        moveObject(aniBigBike);
        Matrix.setIdentityM(mModelMatrix_BigBike, 0);

        Matrix.translateM(mModelMatrix_BigBike, 0, aniBigBike.pos_x, aniBigBike.pos_y, aniBigBike.pos_z);  // center of the building is (120, 80)
        Matrix.scaleM(mModelMatrix_BigBike, 0, SCALE_BIGBIKE, SCALE_BIGBIKE, SCALE_BIGBIKE);

        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix_BigBike, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);
        Matrix.transposeM(mModelViewInvTrans, 0, mModelViewMatrix, 0);
        Matrix.invertM(mModelViewInvTrans, 0, mModelViewInvTrans, 0);

        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewProjectionMatrix, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrix, 1, false, mModelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrixInvTrans, 1, false, mModelViewInvTrans, 0);

        mShadingProgram.setUpMaterialBike();
        mBikeBig.draw();

        // Draw SmallBike
        moveObject(aniSmallBike);
        Matrix.setIdentityM(mModelMatrix_SmallBike, 0);
        Matrix.translateM(mModelMatrix_SmallBike, 0, aniSmallBike.pos_x, aniSmallBike.pos_y, aniSmallBike.pos_z);  // center of the building is (120, 80)
        Matrix.multiplyMM(mModelMatrix_SmallBike, 0, mModelMatrix_BigBike, 0, mModelMatrix_SmallBike, 0);
        Matrix.scaleM(mModelMatrix_SmallBike, 0, SCALE_SMALLBIKE, SCALE_SMALLBIKE, SCALE_SMALLBIKE);
        Matrix.rotateM(mModelMatrix_SmallBike, 0, aniSmallBike.rot_angle, 0f, 1.0f, 0f);

        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix_SmallBike, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);
        Matrix.transposeM(mModelViewInvTrans, 0, mModelViewMatrix, 0);
        Matrix.invertM(mModelViewInvTrans, 0, mModelViewInvTrans, 0);

        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewProjectionMatrix, 1, false, mMVPMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrix, 1, false, mModelViewMatrix, 0);
        GLES30.glUniformMatrix4fv(mShadingProgram.locModelViewMatrixInvTrans, 1, false, mModelViewInvTrans, 0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mBikeSmall.mTexId[0]);
        GLES30.glUniform1i(mShadingProgram.locTexture, TEXTURE_ID_BIKE);

        mShadingProgram.setUpMaterialBike();
        mBikeSmall.draw();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height){
        GLES30.glViewport(0, 0, width, height);

        ratio = (float)width / height;

        Matrix.perspectiveM(mProjectionMatrix, 0, mCamera.getFovy(), ratio, 0.1f, 2000.0f);
    }

    static int prevTimeStamp = 0;
    static int currTimeStamp = 0;
    static int totalTimeStamp = 0;

    private int getTimeStamp(){
        Long tsLong = System.currentTimeMillis() / 100;

        currTimeStamp = tsLong.intValue();
        if(prevTimeStamp != 0){
            totalTimeStamp += (currTimeStamp - prevTimeStamp);
        }
        prevTimeStamp = currTimeStamp;

        return totalTimeStamp;
    }

    public void setLight1(){
        mShadingProgram.light[1].light_on = 1 - mShadingProgram.light[1].light_on;
    }

}