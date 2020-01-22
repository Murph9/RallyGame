#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform float m_EdgeWidth;
uniform float m_EdgeIntensity;

uniform float m_NormalThreshold;
uniform float m_DepthThreshold;

uniform float m_NormalSensitivity;
uniform float m_DepthSensitivity;

uniform float m_DarkenFraction;

varying vec2 texCoord;

uniform COLORTEXTURE m_Texture;
uniform sampler2D m_NormalsTexture;
uniform DEPTHTEXTURE m_DepthTexture;

uniform vec2 g_ResolutionInverse;

vec4 fetchPixelValue(vec2 tc) {
    vec4 nd;
    nd.xyz = texture2D(m_NormalsTexture, tc).rgb;
    nd.w   = fetchTextureSample(m_DepthTexture, tc, 0).r;
    return nd;
}


// Does this pixel lie on an edge?
float getEdgeCalc() {
    vec2 edgeOffset = vec2(m_EdgeWidth) * g_ResolutionInverse;

    // diagonal
    vec4 d1 = fetchPixelValue(texCoord + vec2(-1.0, -1.0) * edgeOffset);
    vec4 d2 = fetchPixelValue(texCoord + vec2( 1.0,  1.0) * edgeOffset);
    vec4 d3 = fetchPixelValue(texCoord + vec2(-1.0,  1.0) * edgeOffset);
    vec4 d4 = fetchPixelValue(texCoord + vec2( 1.0, -1.0) * edgeOffset);
    // inline
    vec4 l1 = fetchPixelValue(texCoord + vec2(-1.0,  0.0) * edgeOffset);
    vec4 l2 = fetchPixelValue(texCoord + vec2( 1.0,  0.0) * edgeOffset);
    vec4 l3 = fetchPixelValue(texCoord + vec2( 0.0,  1.0) * edgeOffset);
    vec4 l4 = fetchPixelValue(texCoord + vec2( 0.0, -1.0) * edgeOffset);

    // Work out how much the normal and depth values are changing.
    vec4 diagonalDelta = abs(d1 - d2) + abs(d3 - d4) + abs(l1 - l2) + abs(l3 - l4);

    float normalDelta = dot(diagonalDelta.xyz, vec3(1.0));
    float depthDelta = diagonalDelta.w;

    // Filter out very small changes, in order to produce nice clean results.
    normalDelta = clamp((normalDelta - m_NormalThreshold) * m_NormalSensitivity, 0.0, 1.0);
    depthDelta  = clamp((depthDelta - m_DepthThreshold) * m_DepthSensitivity,    0.0, 1.0);

    return clamp((normalDelta + depthDelta) * m_EdgeIntensity, 0.0, 1.0);
}

void main() {
    float edgeAmount = getEdgeCalc();
    
    vec4 color4 = getColor(m_Texture, texCoord);
    vec3 color3 = color4.rgb;

    // Apply the edge detection result to the main scene color.
    color3 = mix(m_DarkenFraction * color3, color3, edgeAmount);
    
    gl_FragColor = vec4(color3.rgb, color4.a);
}
