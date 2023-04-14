#version 150

in vec4 vertexColor;
in float discardDraw;

uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    if (color.a == 0.0 || discardDraw > 0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
