@import java.util.List
@import ua.edu.ukma.oss.organization.OrganizationDto
@import ua.edu.ukma.oss.web.support.PageContext
@param PageContext ctx
@param List<OrganizationDto> orgs
@param String selected = null
@template.layout.page(ctx = ctx, title = "Нова петиція", section = "petitions", content = @`
    <div class="form-narrow" style="max-width:640px">
        <h1>Нова петиція</h1>
        <p class="muted">Петицію подає студент, який не входить до складу СК чи КС. Її буде розглянуто на окремому засіданні або в порядку денному наявного.</p>
        <form class="card" method="post" action="/petitions">
            <input type="hidden" name="${ctx.csrfParam()}" value="${ctx.csrfToken()}">
            <div class="field">
                <label for="orgId">Кому адресовано</label>
                <select id="orgId" name="orgId" required>
                    @for(var o : orgs)
                        <option value="${o.id()}" selected="${o.code().name().equals(selected)}">${o.name()}</option>
                    @endfor
                </select>
            </div>
            <div class="field">
                <label for="title">Заголовок</label>
                <input id="title" name="title" type="text" maxlength="255" required>
            </div>
            <div class="field">
                <label for="text">Текст петиції</label>
                <textarea id="text" name="text" required></textarea>
            </div>
            <button class="btn" type="submit">Подати</button>
        </form>
    </div>
`)
