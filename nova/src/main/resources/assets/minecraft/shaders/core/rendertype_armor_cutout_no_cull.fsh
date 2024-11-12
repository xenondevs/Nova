#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    
    if (color.a == 0) {
        discard;
    }
    
    color *= vertexColor * ColorModulator;
    vec4 litColor = color * lightMapColor;
    litColor = mix(color, litColor, color.a); // uses alpha value to define emissivity
    fragColor = linear_fog(litColor, vertexDistance, FogStart, FogEnd, FogColor);
}