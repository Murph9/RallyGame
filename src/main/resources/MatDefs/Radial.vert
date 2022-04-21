#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
attribute vec2 inTexCoord;

varying vec2 texCoord;

void main(){
    texCoord = inTexCoord;
    gl_Position = TransformWorldViewProjection(vec4(inPosition, 1.0));
}