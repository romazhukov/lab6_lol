package shared;

import org.lab5.models.Address;
import org.lab5.models.Organization;
import org.lab5.models.OrganizationType;

import java.io.Serializable;
import java.util.Arrays;

public class CommandRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String[] args;
    private final Organization.Draft draft;
    private final Integer targetId;
    private final Address address;
    private final OrganizationType organizationType;

    public CommandRequest(
            String name,
            String[] args,
            Organization.Draft draft,
            Integer targetId,
            Address address,
            OrganizationType organizationType
    ) {
        this.name = name;
        this.args = args == null ? new String[0] : args;
        this.draft = draft;
        this.targetId = targetId;
        this.address = address;
        this.organizationType = organizationType;
    }

    public String getName() {
        return name;
    }

    public String[] getArgs() {
        return args;
    }

    public Organization.Draft getDraft() {
        return draft;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public Address getAddress() {
        return address;
    }

    public OrganizationType getOrganizationType() {
        return organizationType;
    }

    @Override
    public String toString() {
        return "CommandRequest{" +
                "name='" + name + '\'' +
                ", args=" + Arrays.toString(args) +
                ", draft=" + draft +
                ", targetId=" + targetId +
                ", address=" + address +
                ", organizationType=" + organizationType +
                '}';
    }
}
