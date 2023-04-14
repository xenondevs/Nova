#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out float discardDraw;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;

    // Calculate screen position as vec2 between 0 and 1
    vec3 ndc = gl_Position.xyz / gl_Position.w;
    vec2 texCoord = ndc.xy * 0.5 + 0.5;

    // The lore tooltip is only outside bounds for positive x and positive y
    // The following two cases detect the top left and the bottom right vertex (both contain an "out of bounds"-case which prevents this shader from causing issues with the inventory background / loading screen)
    if ((texCoord.x < 0.001 && texCoord.y > 1) || (texCoord.x > 1 && texCoord.y < 0.001)) {
        // the fragment shader receives an interpolated version of this value and will not draw the pixel when discardDraw > 0
        discardDraw = 1;
    } else {
        discardDraw = 0;
    }
}