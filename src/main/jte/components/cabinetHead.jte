@import ua.edu.ukma.oss.organization.OrganizationDto
@param OrganizationDto org
@param String active
!{String code = org.code().name().toLowerCase();}
<div class="breadcrumbs">Кабінет · ${org.name()}</div>
<nav class="tabs">
    <a href="/cabinet/${code}" class="${active.equals("overview") ? "active" : ""}">Огляд</a>
    <a href="/cabinet/${code}/meetings" class="${active.equals("meetings") ? "active" : ""}">Засідання</a>
    <a href="/cabinet/${code}/petitions" class="${active.equals("petitions") ? "active" : ""}">Петиції</a>
    <a href="/cabinet/${code}/protocols" class="${active.equals("protocols") ? "active" : ""}">Протоколи</a>
</nav>
