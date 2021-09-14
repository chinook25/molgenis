package org.molgenis.core.ui.admin.usermanager;

import static java.util.Objects.requireNonNull;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.molgenis.core.ui.settings.FormSettings;
import org.molgenis.web.PluginController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Api("User manager")
@Controller
@RequestMapping(UserManagerController.URI)
public class UserManagerController extends PluginController {
  public static final String URI = PluginController.PLUGIN_URI_PREFIX + "usermanager";
  private final UserManagerService pluginUserManagerService;
  private final FormSettings formSettings;

  public UserManagerController(
      UserManagerService pluginUserManagerService, FormSettings formSettings) {
    super(URI);
    this.formSettings = requireNonNull(formSettings);
    this.pluginUserManagerService = requireNonNull(pluginUserManagerService);
  }

  @ApiOperation("Return user manager view")
  @ApiResponses({@ApiResponse(code = 200, message = "Return the user manager view")})
  @GetMapping
  public String init(Model model) {
    model.addAttribute(
        "activeSessionCount", this.pluginUserManagerService.getActiveSessionsCount());
    model.addAttribute("users", this.pluginUserManagerService.getAllUsers());
    model.addAttribute("activeUsers", this.pluginUserManagerService.getActiveSessionUserNames());
    model.addAttribute("formSettings", this.formSettings);

    return "view-usermanager";
  }

  @ApiOperation("Sets activation status for a user")
  @ApiResponses({
    @ApiResponse(code = 200, message = "Ok", response = ActivationResponse.class),
    @ApiResponse(
        code = 404,
        message = "If response doesn't have success set to true, the user wasn't found",
        response = ActivationResponse.class)
  })
  @PutMapping("/activation")
  @ResponseStatus(HttpStatus.OK)
  @SuppressWarnings("javasecurity:S5131") // activation is validated in pluginManagerService
  public @ResponseBody ActivationResponse activation(@RequestBody Activation activation) {
    ActivationResponse activationResponse = new ActivationResponse();
    activationResponse.setId(activation.getId());
    pluginUserManagerService.setActivationUser(activation.getId(), activation.getActive());
    activationResponse.setSuccess(true);
    return activationResponse;
  }

  public class ActivationResponse {
    private boolean success = false;
    private String id;

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  public class Activation {
    private String id;
    private Boolean active;

    Activation(String id, Boolean active) {
      this.id = id;
      this.active = active;
    }

    /** @return the id */
    public String getId() {
      return id;
    }

    /** @param id the id to set */
    public void setId(String id) {
      this.id = id;
    }

    /** @return the active */
    public Boolean getActive() {
      return active;
    }

    /** @param active the active to set */
    public void setActive(Boolean active) {
      this.active = active;
    }
  }
}
