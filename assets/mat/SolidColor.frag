uniform vec4 m_Color;

void main() {
    //returning the color of the pixel (here solid blue)
    //- gl_FragColor is the standard GLSL variable that holds the pixel
    //color. It must be filled in the Fragment Shader.
    vec4 color = vec4(1.0);
    gl_FragColor = color * m_Color;
}