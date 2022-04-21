varying vec2 texCoord;

uniform sampler2D m_ThresholdMap;
uniform float m_Threshold;
uniform vec4 m_Color;

#if defined(DISCARD_ALPHA)
    uniform float m_AlphaDiscardThreshold;
#endif


void main() {
	vec4 color = vec4(1.0);
	
    if (m_Threshold < texture2D(m_ThresholdMap, texCoord).a) {
    	color = m_Color;
    } else {
    	color = vec4(0);
    }

	#if defined(DISCARD_ALPHA)
        if (color.a < m_AlphaDiscardThreshold) {
           discard;
        }
    #endif

	gl_FragColor = color;
}