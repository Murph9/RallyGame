#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

attribute vec3 inPosition;

varying vec3 worldUV;

void main() {
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);

    // create a world UV to see if we can do it
    worldUV = (g_WorldMatrix * vec4(inPosition, 1.0)).xyz;
}
