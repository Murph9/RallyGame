#import "Common/ShaderLib/GLSLCompat.glsllib"

#if defined(DISCARD_ALPHA)
    uniform float m_AlphaDiscardThreshold;
#endif

uniform float m_RepeatingPatternSize;
uniform vec4 m_Color;

varying vec3 worldUV;

float mymod(float x, float y){
    return x - y*floor(x/y);
}

void main() {
    vec4 color = m_Color;
    float diff = 0.8; //TODO a arg

    #if defined(DISCARD_ALPHA)
        if (color.a < m_AlphaDiscardThreshold) {
           discard;
        }
    #endif

    vec3 _worldUV = vec3(mymod(worldUV.x, m_RepeatingPatternSize), mymod(worldUV.y, m_RepeatingPatternSize), mymod(worldUV.z, m_RepeatingPatternSize));
    float repeatingSizeHalf = 0.5 * m_RepeatingPatternSize;

    #ifdef T_CHECKER
        if (_worldUV.x > repeatingSizeHalf && _worldUV.z < repeatingSizeHalf) {
            color *= diff;
        }
        if (_worldUV.x < repeatingSizeHalf && _worldUV.z > repeatingSizeHalf) {
            color *= diff;
        }
    #endif

    #ifdef T_SQUARED
        if (_worldUV.x < repeatingSizeHalf && _worldUV.z < repeatingSizeHalf) {
            color *= diff;
        }
    #endif

    #ifdef T_PICNIC
        if (_worldUV.x < repeatingSizeHalf) {
            color *= diff;
        }
        if (_worldUV.z < repeatingSizeHalf) {
            color *= diff;
        }
    #endif

    #ifdef T_DIAG_STRIPED
        if (_worldUV.x + _worldUV.z > repeatingSizeHalf && _worldUV.x + _worldUV.z < m_RepeatingPatternSize) {
            color *= diff;
        } else if (_worldUV.x + _worldUV.z > m_RepeatingPatternSize*1.5) {
            color *= diff;
        }
    #endif

    #ifdef T_Xd
        float distance = (_worldUV.x-repeatingSizeHalf)*(_worldUV.x-repeatingSizeHalf) + (_worldUV.z-repeatingSizeHalf)*(_worldUV.z-repeatingSizeHalf);
        if (distance < m_RepeatingPatternSize*1.5 && distance > m_RepeatingPatternSize*0.5) {
            color *= diff;
        } else if (_worldUV.x+_worldUV.z > m_RepeatingPatternSize*0.95 && _worldUV.x+_worldUV.z < m_RepeatingPatternSize*1.05) {
            color *= diff;
        }
    #endif

    gl_FragColor = color;
}
