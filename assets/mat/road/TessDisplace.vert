
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
    	
uniform vec4 g_LightColor;
uniform vec4 g_LightPosition;
uniform vec4 g_AmbientLightColor;

uniform mat4 g_ProjectionMatrix;
uniform vec3 g_CameraPosition;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;
attribute vec4 inTangent;

out vec2 TexCoord_CS_in;
out vec3 Normal_CS_in;
out vec3 WorldPos_CS_in;
out vec4 Tangent_CS_in;
out vec3 CamPos;

#ifdef HEIGHTMAP
   uniform sampler2D m_NormalDisplacementMap;
#endif


void main(){
    Tangent_CS_in = (g_ProjectionMatrix * inTangent);
    WorldPos_CS_in = (g_WorldViewMatrix*vec4(inPosition, 1.0)).xyz;
    TexCoord_CS_in = inTexCoord;
    Normal_CS_in = (g_ProjectionMatrix * vec4(inNormal, 0.0)).xyz;
    
    gl_Position= vec4(inPosition,1);

}