#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform vec4 m_LowColor;
uniform vec4 m_HighColor;

uniform float m_Offset;
uniform float m_Scale;

varying float height;

void main(void) {
    vec4 color = mix(m_LowColor, m_HighColor, (height-m_Offset)/m_Scale);
    gl_FragColor = vec4(color.rgb, 1.0); // not sure why this needs alpha to be set to 1
}
