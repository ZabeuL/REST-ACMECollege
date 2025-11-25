/**
 * File:  SecurityRoleSerializer.java Course materials (23W) CST 8277
 *
 * @author Teddy Yap
 * 
 * Updated by:  Group NN
 *   Lucas, Subhechha, David, Abhiram
 *
 */
package acmecollege.rest.serializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import acmecollege.entity.SecurityRole;

public class SecurityRoleSerializer extends StdSerializer<Set<SecurityRole>> implements Serializable {
    private static final long serialVersionUID = 1L;

    public SecurityRoleSerializer() {
        this(null);
    }

    public SecurityRoleSerializer(Class<Set<SecurityRole>> t) {
        super(t);
    }

    @Override
    public void serialize(Set<SecurityRole> roles, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Set<String> roleNames = new HashSet<>();
        for (SecurityRole role : roles) {
            roleNames.add(role.getRoleName());
        }
        gen.writeObject(roleNames);
    }
}
