#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
varying float height;

void main() {
    height = inPosition.y;
    gl_Position = TransformWorldViewProjection(vec4(inPosition, 1.0));
}
