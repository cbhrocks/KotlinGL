#version 330 core

in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vFragPos;

out vec4 FragColor;

uniform vec3 uViewPos;

struct Material {
    sampler2D diffuse;
    sampler2D normalMap;
    sampler2D specular;
    vec3 baseColor;
    float reflect;
    float shininess;
    vec2 uvScale;
};

uniform Material material;

uniform vec3 uLightDir = normalize(vec3(-0.5, -1.0, -0.3));
uniform vec3 uLightColor = vec3(1.0);

void main() {
    vec2 scaledUV = vTexCoord * material.uvScale;

    // Sample textures
    vec3 diffuseColor = texture(material.diffuse, scaledUV).rgb;
    vec3 specularColor = texture(material.specular, scaledUV).rgb;

    // Normal mapping
    vec3 sampledNormal = texture(material.normalMap, scaledUV).rgb;
    sampledNormal = normalize(sampledNormal * 2.0 - 1.0); // Convert from [0,1] to [-1,1]

    // Lighting calculations
    vec3 lightDir = normalize(-uLightDir);
    vec3 viewDir = normalize(uViewPos - vFragPos);
    vec3 halfDir = normalize(lightDir + viewDir);

    float diff = max(dot(sampledNormal, lightDir), 0.0);
    float spec = pow(max(dot(sampledNormal, halfDir), 0.0), material.shininess);

    // Fallback for baseColor if diffuse map is flat or black (optional)
    vec3 base = length(diffuseColor) < 0.001 ? material.baseColor : diffuseColor;

    vec3 color = uLightColor * (
    base * diff +
    specularColor * spec * material.reflect
    );

    // FragColor = vec4(1.0, 1.0, 1.0, 1.0);
    FragColor = vec4(color, 1.0);
}
