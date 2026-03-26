import { NavLink } from 'react-router-dom';

type Props = {
  recommendHref?: string;
};

export default function InvestFeatureTabs({ recommendHref = '/ai-recommend' }: Props) {
  return (
    <nav className="stock-page-tabs invest-feature-tabs">
      <NavLink to="/stocks" className={({ isActive }) => `stock-page-tab${isActive ? ' active' : ''}`}>해외 주식</NavLink>
      <NavLink to="/kr-stocks" className={({ isActive }) => `stock-page-tab${isActive ? ' active' : ''}`}>국내 주식</NavLink>
      <NavLink to={recommendHref} className={({ isActive }) => `stock-page-tab${isActive ? ' active' : ''}`}>AI 추천</NavLink>
      <NavLink to="/ai-invest" className={({ isActive }) => `stock-page-tab${isActive ? ' active' : ''}`}>AI 투자</NavLink>
    </nav>
  );
}
