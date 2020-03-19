uniform float m_Offset;
uniform float m_Scale;

varying float height;

void main(void) {
    vec3 c1 = vec3(1.0,0.55,0.0);
    vec3 c2 = vec3(0.0,0.0,1.0);

    gl_FragColor = vec4(mix(c1, c2, (height-m_Offset)/m_Scale), 0);
}
