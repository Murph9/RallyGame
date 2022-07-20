#import "Common/ShaderLib/GLSLCompat.glsllib"

#if defined(DISCARD_ALPHA)
    uniform float m_AlphaDiscardThreshold;
#endif

#if defined(CROSS_HATCH)
    uniform float m_RepeatingPatternSize;
#endif

uniform vec4 m_Color;
uniform vec4 m_RepeatingColour;

varying vec3 worldUV;

float mymod(float x, float y){
    return x - y*floor(x/y);
}

void main() {
    vec4 color = m_Color;

    #if defined(DISCARD_ALPHA)
        if (color.a < m_AlphaDiscardThreshold) {
           discard;
        }
    #endif

    #if defined(CROSS_HATCH)
        vec3 _worldUV = vec3(mymod(worldUV.x, m_RepeatingPatternSize), mymod(worldUV.y, m_RepeatingPatternSize), mymod(worldUV.z, m_RepeatingPatternSize));
        float repeatingSizeHalf = 0.5 * m_RepeatingPatternSize;
        if (_worldUV.x > repeatingSizeHalf && _worldUV.z < repeatingSizeHalf)
            color *= 0.8;//m_RepeatingColour;
        if (_worldUV.x < repeatingSizeHalf && _worldUV.z > repeatingSizeHalf)
            color *= 0.8;//m_RepeatingColour;
    #endif
    
    gl_FragColor = color;
}
