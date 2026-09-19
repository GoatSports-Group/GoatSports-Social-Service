package iuh.fit.socialservice.presentation.converter;

import iuh.fit.socialservice.application.dto.response.UserFollowStatusResponseCommand;
import iuh.fit.socialservice.application.dto.response.VenueFollowStatusResponseCommand;
import iuh.fit.socialservice.presentation.response.UserFollowStatusResponse;
import iuh.fit.socialservice.presentation.response.VenueFollowStatusResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SocialFollowConverter {
    VenueFollowStatusResponse toResponse(VenueFollowStatusResponseCommand command);

    UserFollowStatusResponse toResponse(UserFollowStatusResponseCommand command);
}
