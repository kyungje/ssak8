
package cleaningServiceydp.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="kakao", url="${api.url.kakao}")
public interface KakaoService {

    @RequestMapping(method= RequestMethod.POST, path="/kakaos")
    public void kakaoAlert(@RequestBody Kakao kakao);

}