uniform mat4 g_WorldViewProjectionMatrix;
attribute vec3 inPosition;

varying float height;

void main() {
    height = inPosition.y;
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
